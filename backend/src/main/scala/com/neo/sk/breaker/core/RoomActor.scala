package com.neo.sk.breaker.core


import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Flow
import com.neo.sk.breaker.common.{AppSettings, Constants}
import com.neo.sk.breaker.core.RoomManager.Command
import com.neo.sk.breaker.core.game.GameContainerServerImpl
import com.neo.sk.breaker.protocol.ActorProtocol.{GameOver, UserJoinRoom, LeftRoom}
import com.neo.sk.breaker.shared.model.Constants.GameState
import com.neo.sk.breaker.shared.protocol
import com.neo.sk.breaker.shared.protocol.BreakerEvent
import org.slf4j.LoggerFactory
//import com.neo.sk.breaker.core.game.GameContainerServerImpl

import concurrent.duration._
import scala.collection.mutable
import com.neo.sk.breaker.Boot.{executor}
import org.seekloud.byteobject.MiddleBufferInJvm

/**
  * Created by sky
  * Date on 2019/2/14
  * Time at 下午6:26
  */
object RoomActor {
  import scala.language.implicitConversions
  import org.seekloud.byteobject.ByteObject._

  private val log = LoggerFactory.getLogger(this.getClass)

  private final val InitTime = Some(5.minutes)

  private final case object BehaviorChangeKey

  private final case object GameLoopKey

  private final case object GameWaitKey

  trait Command

  case object GameLoop extends Command
  case object GameWaitOut extends Command

  case class WebSocketMsg(req: BreakerEvent.UserActionEvent) extends Command with RoomManager.Command

  final case class SwitchBehavior(
                                   name: String,
                                   behavior: Behavior[Command],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut = TimeOut("busy time error")
                                 ) extends Command

  case class TimeOut(msg: String) extends Command

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None, timeOut: TimeOut = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer: TimerScheduler[Command]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }

  def create(roomId: Long): Behavior[Command] = {
    log.debug(s"RoomActor-${roomId} start...")
    Behaviors.setup[Command] {
      ctx =>
        implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
        Behaviors.withTimers[Command] {
          implicit timer =>
            val subscribersMap = mutable.HashMap[String, (ActorRef[UserActor.Command],ActorRef[BreakerEvent.WsMsgSource])]()
            implicit val sendBuffer = new MiddleBufferInJvm(81920)
            val gameContainer = GameContainerServerImpl(AppSettings.breakerGameConfig,log,ctx.self,dispatch(subscribersMap)
            )
            switchBehavior(ctx,"wait",wait(roomId, subscribersMap, gameContainer))
        }
    }
  }

  private def wait(
                  roomId:Long,
                  subscribersMap: mutable.HashMap[String, (ActorRef[UserActor.Command],ActorRef[BreakerEvent.WsMsgSource])],
                  gameContainer: GameContainerServerImpl
                  )(
                    implicit timer: TimerScheduler[Command],
                    sendBuffer: MiddleBufferInJvm,
                    stashBuffer: StashBuffer[Command]
                  ): Behavior[Command] = {
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case UserJoinRoom(userInfo,userActor,frontActor) =>
          if(subscribersMap.size<2){
            //remind 此处顺序不可变
            if(subscribersMap.isEmpty){
              timer.startSingleTimer(GameWaitKey,GameWaitOut,1.minutes)
            }
            gameContainer.joinGame(userInfo.playerId.getOrElse(""), userInfo.nickName,subscribersMap.nonEmpty, userActor,frontActor)
            subscribersMap.put(userInfo.playerId.getOrElse(""),(userActor,frontActor))
          }
          if(subscribersMap.size>1){
            timer.cancel(GameWaitKey)
            log.info(s"room-$roomId start....")
            gameContainer.startGame
            //remind 同步全量数据
            //            dispatch(subscribersMap)(BreakerEvent.SyncGameAllState(gameContainer.getGameContainerAllState()))
            subscribersMap.values.foreach(_._1 ! UserActor.DispatchMsg(BreakerEvent.Wrap(BreakerEvent.SyncGameAllState(gameContainer.getGameContainerAllState()).asInstanceOf[BreakerEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())))
            timer.startPeriodicTimer(GameLoopKey, GameLoop, gameContainer.config.frameDuration.millis)
            switchBehavior(ctx,"idle",idle(roomId, subscribersMap, gameContainer))
          }else{
            Behaviors.same
          }

        case msg:LeftRoom=>
          log.debug(s"roomActor left room:${msg.userInfo.playerId} in wait")
          Behaviors.stopped

        case GameWaitOut=>
          //todo 添加Bot
          dispatch(subscribersMap)(BreakerEvent.GameWaitOut)
          subscribersMap.foreach(_._2._1 ! UserActor.ChangeBehaviorToInit(true))
          Behaviors.stopped

        case unKnow =>
          log.warn(s"${ctx.self.path} recv a unknow msg=${msg}")
          stashBuffer.stash(unKnow)
          Behaviors.same
      }
      }
    }

  private def idle(
            roomId: Long,
            subscribersMap: mutable.HashMap[String, (ActorRef[UserActor.Command],ActorRef[BreakerEvent.WsMsgSource])],
            gameContainer: GameContainerServerImpl
          )(
            implicit timer: TimerScheduler[Command],
            sendBuffer: MiddleBufferInJvm,
            stashBuffer: StashBuffer[Command]
          ): Behavior[Command] = {
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case GameLoop =>
          gameContainer.update()
          if(gameContainer.systemFrame%20==0){
            dispatch(subscribersMap)(BreakerEvent.SyncGameState(gameContainer.getGameContainerState(false)))
          }
          if(gameContainer.systemFrame%50==0){
            dispatch(subscribersMap)(BreakerEvent.SyncGameState(gameContainer.getGameContainerState(true)))
          }
          Behaviors.same

        case WebSocketMsg(req) =>
          gameContainer.receiveUserAction(req)
          Behaviors.same

        case GameOver=>
          subscribersMap.values.foreach(_._1 ! GameOver)
          Behaviors.stopped

        case msg:LeftRoom=>
          log.debug(s"roomActor left room:${msg.userInfo.playerId}")
          gameContainer.leftGame(msg.userInfo.playerId.getOrElse(""))
          subscribersMap.remove(msg.userInfo.playerId.getOrElse(""))
          Behaviors.same

        case unKnow =>
          log.warn(s"${ctx.self.path} recv a unknow msg=${msg}")
          Behaviors.same
      }
    }
  }

  def dispatch(subscribers: mutable.HashMap[String, (ActorRef[UserActor.Command],ActorRef[BreakerEvent.WsMsgSource])])(msg: BreakerEvent.WsMsgServer)(implicit sendBuffer: MiddleBufferInJvm) = {
    subscribers.values.foreach(_._2 ! BreakerEvent.Wrap(msg.asInstanceOf[BreakerEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result()))
//    subscribers.values.foreach(_._1 ! UserActor.DispatchMsg(BreakerEvent.Wrap(msg.asInstanceOf[BreakerEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())))
  }
}

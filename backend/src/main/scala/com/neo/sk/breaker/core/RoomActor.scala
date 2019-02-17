package com.neo.sk.breaker.core


import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Flow
import com.neo.sk.breaker.common.{AppSettings, Constants}
import com.neo.sk.breaker.core.RoomManager.Command
import com.neo.sk.breaker.core.game.GameContainerServerImpl
import com.neo.sk.breaker.protocol.ActorProtocol.JoinRoom
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
  private val log = LoggerFactory.getLogger(this.getClass)

  private final val InitTime = Some(5.minutes)

  private val breakIdGenerator = new AtomicInteger(100)

  private final case object BehaviorChangeKey

  private final case object GameLoopKey

  trait Command

  case object GameLoop extends Command

  case object GameStopRoom extends Command

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
    log.debug(s"Room Actor-${roomId} start...")
    Behaviors.setup[Command] {
      ctx =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            val subscribersMap = mutable.HashMap[String, (ActorRef[UserActor.Command],ActorRef[BreakerEvent.WsMsgSource])]()
            implicit val sendBuffer = new MiddleBufferInJvm(81920)
            val gameContainer = GameContainerServerImpl(AppSettings.breakerGameConfig,log,ctx.self,dispatch(subscribersMap)
            )
            idle(roomId, subscribersMap, gameContainer)
        }
    }
  }

  def idle(
            roomId: Long,
            subscribersMap: mutable.HashMap[String, (ActorRef[UserActor.Command],ActorRef[BreakerEvent.WsMsgSource])],
            gameContainer: GameContainerServerImpl
          )(
            implicit timer: TimerScheduler[Command],
            sendBuffer: MiddleBufferInJvm
          ): Behavior[Command] = {
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case JoinRoom(userInfo, userActor,frontActor) =>
          if(subscribersMap.size<2){
            subscribersMap.put(userInfo.playerId.getOrElse(""),(userActor,frontActor))
            gameContainer.joinGame(userInfo.playerId.getOrElse(""), userInfo.nickName,breakIdGenerator.getAndIncrement(), userActor,frontActor)
          }
          if(subscribersMap.size>1){
            log.info(s"room-$roomId start....")
            gameContainer.startGame
            timer.startPeriodicTimer(GameLoopKey, GameLoop, gameContainer.config.frameDuration.millis)
          }
          idle(roomId, subscribersMap, gameContainer)

        case GameLoop =>

          Behaviors.same

        case _ =>
          log.warn(s"${ctx.self.path} recv a unknow msg=${msg}")
          Behaviors.same
      }
    }
  }

  import scala.language.implicitConversions
  import org.seekloud.byteobject.ByteObject._

  def dispatch(subscribers: mutable.HashMap[String, (ActorRef[UserActor.Command],ActorRef[BreakerEvent.WsMsgSource])])(msg: BreakerEvent.WsMsgServer)(implicit sendBuffer: MiddleBufferInJvm) = {
    subscribers.values.foreach(_._2 ! protocol.BreakerEvent.Wrap(msg.asInstanceOf[BreakerEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result()))
  }
}

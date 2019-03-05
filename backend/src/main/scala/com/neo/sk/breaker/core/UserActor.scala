package com.neo.sk.breaker.core

import akka.actor.Terminated
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import com.neo.sk.breaker.common.{AppSettings, Constants}
import com.neo.sk.breaker.core.game.BreakGameConfigServerImpl
import com.neo.sk.breaker.shared.`object`.Breaker
import com.neo.sk.breaker.shared.protocol.{BreakerEvent, UserProtocol}
import org.seekloud.byteobject.MiddleBufferInJvm
import com.neo.sk.breaker.shared.protocol.BreakerEvent._
import org.slf4j.LoggerFactory
import com.neo.sk.breaker.Boot.{executor,roomManager,timeout,scheduler}
import scala.concurrent.duration._
import scala.language.implicitConversions
import com.neo.sk.breaker.protocol.ActorProtocol

import org.seekloud.byteobject.ByteObject._
/**
  * Created by sky
  * Date on 2019/2/14
  * Time at 下午1:59
  */
object UserActor {
  private val log = LoggerFactory.getLogger(this.getClass)
  private final val InitTime = Some(5.minutes)

  trait Command

  @deprecated
  case class DispatchMsg(msg:BreakerEvent.WsMsgSource) extends Command

  case class WebSocketMsg(reqOpt: Option[BreakerEvent.WsMsgFront]) extends Command

  case object CompleteMsgFront extends Command

  case class FailMsgFront(ex: Throwable) extends Command

  /**此消息用于外部控制状态转入初始状态，以便于重建WebSocket*/
  final case class ChangeBehaviorToInit(stop:Boolean) extends Command

  case class UserFrontActor(actor:ActorRef[BreakerEvent.WsMsgSource]) extends Command

  case object WsCreateSuccess extends Command

  case class UserLeft[U](actorRef:ActorRef[U]) extends Command

  private final case object BehaviorChangeKey

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

  private def sink(actor: ActorRef[Command]) = ActorSink.actorRef[Command](
    ref = actor,
    onCompleteMessage = CompleteMsgFront,
    onFailureMessage = FailMsgFront.apply
  )

  def flow(actor: ActorRef[UserActor.Command]): Flow[WebSocketMsg, BreakerEvent.WsMsgSource, Any] = {
    val in = Flow[WebSocketMsg].to(sink(actor))
    val out =
      ActorSource.actorRef[BreakerEvent.WsMsgSource](
        completionMatcher = {
          case BreakerEvent.CompleteMsgServer ⇒
        },
        failureMatcher = {
          case BreakerEvent.FailMsgServer(e) ⇒ e
        },
        bufferSize = 128,
        overflowStrategy = OverflowStrategy.dropHead
      ).mapMaterializedValue(outActor => actor ! UserFrontActor(outActor))
    Flow.fromSinkAndSource(in, out)
  }

  def create(userInfo: UserProtocol.UserInfo): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      log.debug(s"${userInfo.playerId.getOrElse("unKnow")} is starting...")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        implicit val sendBuffer = new MiddleBufferInJvm(8192)
        switchBehavior(ctx, "init", init(userInfo), InitTime, TimeOut("init"))
      }
    }
  }

  /**等待建立链接状态*/
  private def init(userInfo: UserProtocol.UserInfo)(
    implicit stashBuffer: StashBuffer[Command],
    sendBuffer: MiddleBufferInJvm,
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case UserFrontActor(frontActor) =>
          ctx.watchWith(frontActor, UserLeft(frontActor))
          switchBehavior(ctx, "idle", idle(userInfo, frontActor))

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          Behaviors.stopped

        case msg:ChangeBehaviorToInit=>
          if(msg.stop){
            Behaviors.stopped
          }else{
            Behaviors.same
          }

        case msg:TimeOut=>
          log.debug(s"${ctx.self.path} is time out when busy,msg=${msg.msg}")
          Behaviors.stopped

        case unKnowMsg =>
          stashBuffer.stash(unKnowMsg)
          log.warn(s"init got unknown msg: $unKnowMsg")
          Behavior.same
      }
    }

  /**等待进入游戏状态*/
  private def idle(userInfo: UserProtocol.UserInfo, frontActor:ActorRef[BreakerEvent.WsMsgSource])(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command],
    sendBuffer:MiddleBufferInJvm
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case WsCreateSuccess =>
          frontActor ! BreakerEvent.Wrap(BreakerEvent.WsSuccess.asInstanceOf[BreakerEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
          Behaviors.same

        case WebSocketMsg(reqOpt) =>
          reqOpt match {
            case Some(BreakerEvent.StartGame) =>
              log.info(s"$userInfo get ws msg startGame")
              roomManager ! ActorProtocol.UserJoinRoom(userInfo,ctx.self,frontActor)

            case Some(t:BreakerEvent.PingPackage) =>
              frontActor ! BreakerEvent.Wrap(t.asInstanceOf[BreakerEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
              Behaviors.same

            case _ =>
          }
          Behaviors.same

        case msg:ActorProtocol.UserJoinRoomSuccess=>
          frontActor ! BreakerEvent.Wrap(BreakerEvent.YourInfo(msg.breaker.getBreakState(),msg.config).asInstanceOf[BreakerEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
          switchBehavior(ctx,"play",play(userInfo,frontActor,msg.roomActor))


        case UserLeft(actor) =>
          ctx.unwatch(actor)
          roomManager ! ActorProtocol.LeftRoom(userInfo)
          Behaviors.stopped

        case msg:ChangeBehaviorToInit=>
          if(msg.stop){
            Behaviors.stopped
          }else{
            frontActor ! BreakerEvent.Wrap(BreakerEvent.RebuildWebSocket.asInstanceOf[BreakerEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
            ctx.unwatch(frontActor)
            roomManager ! ActorProtocol.LeftRoom(userInfo)
            switchBehavior(ctx, "init", init(userInfo))
          }

        case msg:TimeOut=>
          log.debug(s"${ctx.self.path} is time out when busy,msg=${msg.msg}")
          Behaviors.stopped

        case unKnowMsg =>
          stashBuffer.stash(unKnowMsg)
          log.warn(s"idle got unknown msg: $unKnowMsg")
          Behavior.same
      }
    }

  private def play(
                    userInfo: UserProtocol.UserInfo,
                    frontActor:ActorRef[BreakerEvent.WsMsgSource],
                    roomActor: ActorRef[RoomActor.Command])(
                    implicit stashBuffer:StashBuffer[Command],
                    timer:TimerScheduler[Command],
                    sendBuffer:MiddleBufferInJvm
                  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case WebSocketMsg(reqOpt) =>
          if(reqOpt.nonEmpty){
            reqOpt.get match{
              case BreakerEvent.StopWebSocket=>
                ctx.unwatch(frontActor)
                roomManager ! ActorProtocol.LeftRoom(userInfo)
                Behaviors.stopped

              case t:BreakerEvent.UserActionEvent =>
                roomActor ! RoomActor.WebSocketMsg(t)
                Behaviors.same

              case t: BreakerEvent.PingPackage =>
                frontActor ! BreakerEvent.Wrap(t.asInstanceOf[BreakerEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
                Behaviors.same

              case _ =>
                Behaviors.same
            }
          }else{
            Behaviors.same
          }

        case msg:DispatchMsg=>
          frontActor ! msg.msg
          Behaviors.same

        case ActorProtocol.GameOver=>
          switchBehavior(ctx, "idle", idle(userInfo, frontActor))

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          roomManager ! ActorProtocol.LeftRoom(userInfo)
          Behaviors.stopped

        case msg:ChangeBehaviorToInit=>
          if(msg.stop){
            Behaviors.stopped
          }else{
            frontActor ! BreakerEvent.Wrap(BreakerEvent.RebuildWebSocket.asInstanceOf[BreakerEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
            ctx.unwatch(frontActor)
            roomManager ! ActorProtocol.LeftRoom(userInfo)
            switchBehavior(ctx, "init", init(userInfo))
          }

        case msg:TimeOut=>
          log.debug(s"${ctx.self.path} is time out when busy,msg=${msg.msg}")
          Behaviors.stopped

        case unKnowMsg =>
          stashBuffer.stash(unKnowMsg)
          log.warn(s"play got unknown msg: $unKnowMsg")
          Behavior.same
      }
    }
}

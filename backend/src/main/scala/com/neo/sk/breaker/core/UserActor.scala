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

  case class WebSocketMsg(reqOpt: Option[BreakerEvent.WsMsgFront]) extends Command

  case object CompleteMsgFront extends Command

  case class FailMsgFront(ex: Throwable) extends Command

  /**此消息用于外部控制状态转入初始状态，以便于重建WebSocket*/
  case object ChangeBehaviorToInit extends Command

  case class UserFrontActor(actor:ActorRef[BreakerEvent.WsMsgSource]) extends Command

  case class WsCreateSuccess(roomId:Option[Long]) extends Command

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
      log.debug(s"${ctx.self.path} is starting...")
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

        case unKnowMsg =>
          stashBuffer.stash(unKnowMsg)
          log.warn(s"got unknown msg: $unKnowMsg")
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
        case msg:WsCreateSuccess =>
          frontActor ! BreakerEvent.Wrap(BreakerEvent.WsSuccess(msg.roomId).asInstanceOf[BreakerEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
          Behaviors.same

        case WebSocketMsg(reqOpt) =>
          reqOpt match {
            case Some(t:BreakerEvent.StartGame) =>
              log.info("get ws msg startGame")
              roomManager ! ActorProtocol.JoinRoom(userInfo,ctx.self,frontActor)
              idle(userInfo,frontActor)
            case _ =>
              Behaviors.same
          }

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          Behaviors.stopped

        case unKnowMsg =>
          stashBuffer.stash(unKnowMsg)
          log.warn(s"got unknown msg: $unKnowMsg")
          Behavior.same
      }
    }
}

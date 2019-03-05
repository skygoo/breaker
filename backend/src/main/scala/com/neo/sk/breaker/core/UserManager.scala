package com.neo.sk.breaker.core

import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.japi
import akka.stream.{ActorAttributes, Supervision}
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.neo.sk.breaker.common.Constants
import com.neo.sk.breaker.core.game.BreakGameConfigServerImpl
import com.neo.sk.breaker.shared.protocol.BreakerEvent
import org.seekloud.byteobject.MiddleBufferInJvm
import org.slf4j.LoggerFactory
import com.neo.sk.breaker.shared.protocol.UserProtocol

/**
  * Created by sky
  * Date on 2019/2/13
  * Time at 下午5:02
  */
object UserManager {
  import io.circe.generic.auto._
  import io.circe.syntax._
  import org.seekloud.byteobject.ByteObject._
  import org.seekloud.byteobject.MiddleBufferInJvm

  private val log = LoggerFactory.getLogger(this.getClass)
  trait Command
  final case class GetWebSocketFlow(replyTo:ActorRef[Flow[Message,Message,Any]], playerInfo:UserProtocol.UserInfo) extends Command

  def create(): Behavior[Command] = {
    log.debug(s"UserManager start...")
    Behaviors.setup[Command] {
      ctx =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            val uidGenerator = new AtomicLong(1L)
            idle(uidGenerator)
        }
    }
  }

  private def idle(uidGenerator: AtomicLong)
                  (
                    implicit timer: TimerScheduler[Command]
                  ): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case GetWebSocketFlow(replyTo, playerInfo) =>
          println(msg)
          //remind 确保playerId
          playerInfo.playerId match {
            case Some(value)=>
              val userActor=getUserActor(ctx,playerInfo)
              userActor ! UserActor.ChangeBehaviorToInit(false)
              replyTo ! getWebSocketFlow(userActor)
              userActor ! UserActor.WsCreateSuccess
            case None=>
              val uid=playerInfo.userName match {
                case Some(value)=>
                  Constants.BreakerSignUserIdPrefix+value
                case None=>
                  Constants.BreakerGameUserIdPrefix+uidGenerator.getAndIncrement().toString
              }
              val userActor=getUserActor(ctx,playerInfo.copy(playerId = Some(uid)))
              userActor ! UserActor.ChangeBehaviorToInit(false)
              replyTo ! getWebSocketFlow(userActor)
              userActor ! UserActor.WsCreateSuccess
          }
          Behaviors.same
      }
    }
  }

  private def getWebSocketFlow(userActor:ActorRef[UserActor.Command]): Flow[Message, Message, Any] = {
    import scala.language.implicitConversions
    import org.seekloud.byteobject.ByteObject._


    implicit def parseJsonString2WsMsgFront(s: String): Option[BreakerEvent.WsMsgFront] = {
      import io.circe.generic.auto._
      import io.circe.parser._

      try {
        val wsMsg = decode[BreakerEvent.WsMsgFront](s).right.get
        Some(wsMsg)
      } catch {
        case e: Exception =>
          log.warn(s"parse front msg failed when json parse,s=${s}")
          None
      }
    }

    Flow[Message]
      .collect {
        case TextMessage.Strict(m) =>
          UserActor.WebSocketMsg(m)

        case BinaryMessage.Strict(m) =>
          val buffer = new MiddleBufferInJvm(m.asByteBuffer)
          bytesDecode[BreakerEvent.WsMsgFront](buffer) match {
            case Right(req) => UserActor.WebSocketMsg(Some(req))
            case Left(e) =>
              log.error(s"decode binaryMessage failed,error:${e.message}")
              UserActor.WebSocketMsg(None)
          }
      }.via(UserActor.flow(userActor))
      .map {
        case t: BreakerEvent.Wrap =>
          BinaryMessage.Strict(ByteString(t.ws))

        case x =>
          log.debug(s"akka stream receive unknown msg=$x")
          TextMessage.apply("")
      }.withAttributes(ActorAttributes.supervisionStrategy(decider))

  }

  private val decider: Supervision.Decider = {
    e: Throwable =>
      e.printStackTrace()
      log.error(s"WS stream failed with $e")
      Supervision.Resume
  }


  private def getUserActor(ctx: ActorContext[Command],userInfo: UserProtocol.UserInfo):ActorRef[UserActor.Command] = {
    val childName = s"UserActor-${userInfo.playerId.getOrElse("")}"
    ctx.child(childName).getOrElse{
      ctx.spawn(UserActor.create(userInfo),childName)
    }.upcast[UserActor.Command]
  }
}

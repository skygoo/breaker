package com.neo.sk.breaker.core

import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import com.neo.sk.breaker.protocol.ActorProtocol.{JoinRoom, JoinRoomFail}
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
  * Created by sky
  * Date on 2019/2/13
  * Time at 下午5:02
  */
object RoomManager {
  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  private case class TimeOut(msg: String) extends Command

  private case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command

  case class CreateRoom(uid: String, tankIdOpt: Option[Int], name: String, startTime: Long, userActor: ActorRef[UserActor.Command], roomId: Option[Long]) extends Command

  def create(): Behavior[Command] = {
    log.debug(s"RoomManager start...")
    Behaviors.setup[Command] {
      ctx =>
        implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
        Behaviors.withTimers[Command] { implicit timer =>
          val roomIdGenerator = new AtomicLong(1L)
          val roomInUse = mutable.HashMap((1l, List.empty[(String, String)]))
          idle(roomIdGenerator, roomInUse)
        }
    }
  }

  def idle(roomIdGenerator: AtomicLong,
           roomInUse: mutable.HashMap[Long, List[(String, String)]]) // roomId => List[userId, nickName]
          (implicit stashBuffer: StashBuffer[Command], timer: TimerScheduler[Command]) = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case JoinRoom(userInfo, userActor,frontActor) =>
          roomInUse.find(p => p._2.length < 2).toList.sortBy(_._1).headOption match {
            case Some(t) =>
              roomInUse.put(t._1, (userInfo.playerId.getOrElse(""), userInfo.nickName) :: t._2)
              getRoomActor(ctx, t._1) ! JoinRoom(userInfo, userActor,frontActor)
            case None =>
              var roomId = roomIdGenerator.getAndIncrement()
              while (roomInUse.exists(_._1 == roomId)) roomId = roomIdGenerator.getAndIncrement()
              roomInUse.put(roomId, List((userInfo.playerId.getOrElse(""), userInfo.nickName)))
              getRoomActor(ctx, roomId) ! JoinRoom(userInfo, userActor,frontActor)
          }
          log.debug(s"now roomInUse:$roomInUse")
          Behaviors.same
      }
    }
  }

  private def getRoomActor(ctx: ActorContext[Command], roomId: Long) = {
    val childName = s"room_$roomId"
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(RoomActor.create(roomId), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor

    }.upcast[RoomActor.Command]
  }
}

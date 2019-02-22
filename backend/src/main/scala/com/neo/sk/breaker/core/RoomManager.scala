package com.neo.sk.breaker.core

import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import com.neo.sk.breaker.protocol.ActorProtocol.{JoinRoom, JoinRoomFail,LeftRoom}
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
          val roomInUse = mutable.HashMap[Long, (Byte,List[(String, String)])]()
          idle(roomIdGenerator, roomInUse)
        }
    }
  }

  def idle(roomIdGenerator: AtomicLong,
           roomInUse: mutable.HashMap[Long, (Byte,List[(String, String)])]) // roomId => List[playerId, nickName]
          (implicit stashBuffer: StashBuffer[Command], timer: TimerScheduler[Command]) = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case msg:JoinRoom =>
          roomInUse.find(p => p._2._1==msg.roomType&&p._2._2.length < 2).toList.sortBy(_._1).headOption match {
            case Some(t) =>
              roomInUse.put(t._1, (t._2._1,(msg.userInfo.playerId.getOrElse(""), msg.userInfo.nickName) :: t._2._2))
              getRoomActor(ctx, t._1,t._2._1) ! msg
            case None =>
              var roomId = roomIdGenerator.getAndIncrement()
              while (roomInUse.exists(_._1 == roomId)) roomId = roomIdGenerator.getAndIncrement()
              roomInUse.put(roomId, (msg.roomType,List((msg.userInfo.playerId.getOrElse(""), msg.userInfo.nickName))))
              getRoomActor(ctx, roomId,msg.roomType) ! msg
          }
          log.debug(s"now roomInUse:$roomInUse")
          Behaviors.same

        case msg:LeftRoom=>
          roomInUse.find(_._2._2.exists(_._1 == msg.userInfo.playerId.getOrElse(""))) match{
            case Some(t) =>
              roomInUse.remove(t._1)
              getRoomActor(ctx,t._1,t._2._1) ! msg
              log.debug(s"玩家：${msg.userInfo.playerId}--${msg.userInfo.nickName}")
            case None => log.debug(s"该玩家不在任何房间--${msg.userInfo}")
          }
          Behaviors.same

        case ChildDead(child,childRef)=>
          log.info(child + " is stop")
          Behaviors.same

        case unKnowMsg =>
          stashBuffer.stash(unKnowMsg)
          log.warn(s"got unknown msg: $unKnowMsg")
          Behavior.same
      }
    }
  }

  private def getRoomActor(ctx: ActorContext[Command], roomId: Long,roomType:Byte) = {
    val childName = s"room_$roomId"
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(RoomActor.create(roomId,roomType), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor

    }.upcast[RoomActor.Command]
  }
}

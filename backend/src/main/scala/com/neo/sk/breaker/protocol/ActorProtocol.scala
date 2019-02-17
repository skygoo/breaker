package com.neo.sk.breaker.protocol

import akka.actor.typed.ActorRef
import com.neo.sk.breaker.core.{RoomActor, RoomManager, UserActor}
import com.neo.sk.breaker.core.UserActor
import com.neo.sk.breaker.core.game.BreakGameConfigServerImpl
import com.neo.sk.breaker.shared.`object`.Breaker
import com.neo.sk.breaker.shared.protocol.BreakerEvent
import com.neo.sk.breaker.shared.protocol.UserProtocol.UserInfo
/**
  * Created by sky
  * Date on 2019/2/16
  * Time at 下午2:33
  */
object ActorProtocol {
  case class JoinRoom(userInfo:UserInfo, userActor:ActorRef[UserActor.Command],frontActor:ActorRef[BreakerEvent.WsMsgSource]) extends UserActor.Command with RoomActor.Command with RoomManager.Command
  case class JoinRoomSuccess(breaker:Breaker, config:BreakGameConfigServerImpl, uId:String, roomActor: ActorRef[RoomActor.Command]) extends UserActor.Command with RoomManager.Command
  case class JoinRoomFail(msg:String) extends UserActor.Command

  case class LeftRoom(uid:String,tankId:Int,name:String,userOpt: Option[String]) extends RoomManager.Command

//  case class LeftRoom(uid: String, tankId: Int, name: String, uidSet: List[(String, String)], roomId: Long) extends RoomActor.Command with RoomManager.Command

  case class LeftRoomByKilled(uid: String, tankId: Int, tankLives: Int, name: String) extends RoomActor.Command with RoomManager.Command

}

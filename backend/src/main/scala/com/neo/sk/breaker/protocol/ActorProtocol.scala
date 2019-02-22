package com.neo.sk.breaker.protocol

import akka.actor.typed.ActorRef
import com.neo.sk.breaker.core.{RoomActor, RoomManager, UserActor}
import com.neo.sk.breaker.core.UserActor
import com.neo.sk.breaker.shared.`object`.Breaker
import com.neo.sk.breaker.shared.game.config.{BreakGameConfig, BreakGameConfigImpl}
import com.neo.sk.breaker.shared.protocol.BreakerEvent
import com.neo.sk.breaker.shared.protocol.UserProtocol.UserInfo
/**
  * Created by sky
  * Date on 2019/2/16
  * Time at 下午2:33
  */
object ActorProtocol {
  case class JoinRoom(userInfo:UserInfo,roomType:Byte, userActor:ActorRef[UserActor.Command],frontActor:ActorRef[BreakerEvent.WsMsgSource]) extends UserActor.Command with RoomActor.Command with RoomManager.Command
  case class JoinRoomSuccess(breaker:Breaker, config:BreakGameConfigImpl,roomType:Byte, roomActor: ActorRef[RoomActor.Command]) extends UserActor.Command
  case class JoinRoomFail(msg:String) extends UserActor.Command

  case class LeftRoom(userInfo: UserInfo) extends RoomActor.Command with RoomManager.Command

  case object GameOver extends RoomActor.Command with UserActor.Command
}

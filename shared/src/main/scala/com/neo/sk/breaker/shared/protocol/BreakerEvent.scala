package com.neo.sk.breaker.shared.protocol

/**
  * Created by sky
  * Date on 2019/1/29
  * Time at 下午9:35
  */
object BreakerEvent {
  /**前端建立WebSocket*/
  sealed trait WsMsgFrontSource
  case object CompleteMsgFrontServer extends WsMsgFrontSource
  case class FailMsgFrontServer(ex: Exception) extends WsMsgFrontSource

  sealed trait WsMsgFront extends WsMsgFrontSource

  /**后台建立WebSocket*/
  trait WsMsgSource
  case object CompleteMsgServer extends WsMsgSource
  case class FailMsgServer(ex: Exception) extends WsMsgSource

  sealed trait WsMsgServer extends WsMsgSource

  sealed trait GameEvent {
    val frame:Long
  }

  trait UserEvent extends GameEvent
  trait EnvironmentEvent extends GameEvent  //游戏环境产生事件
  trait UserActionEvent extends UserEvent{   //游戏用户动作事件
    val tankId:Int
    val serialNum:Byte
  }

  final case class EventData(list:List[WsMsgServer]) extends WsMsgServer
  final case object DecodeError extends WsMsgServer
}

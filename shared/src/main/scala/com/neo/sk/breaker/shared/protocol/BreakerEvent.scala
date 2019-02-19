package com.neo.sk.breaker.shared.protocol

import com.neo.sk.breaker.shared.`object`.{BallState, BreakState, ObstacleState}
import com.neo.sk.breaker.shared.game.config.BreakGameConfigImpl

/**
  * Created by sky
  * Date on 2019/1/29
  * Time at 下午9:35
  */
object BreakerEvent {
  final case class GameContainerAllState(
                                          f:Long,
                                          breakers:List[BreakState],
                                          balls:List[BallState],
                                          obstacle:List[ObstacleState],
                                          environment:List[ObstacleState],
                                        )

  final case class GameContainerState(
                                 f:Long
                               )

  /**前端建立WebSocket*/
  sealed trait WsMsgFrontSource
  case object CompleteMsgFrontServer extends WsMsgFrontSource
  case class FailMsgFrontServer(ex: Exception) extends WsMsgFrontSource

  sealed trait WsMsgFront extends WsMsgFrontSource

  final case object StartGame extends WsMsgFront

  /**后台建立WebSocket*/
  trait WsMsgSource
  case object CompleteMsgServer extends WsMsgSource
  case class FailMsgServer(ex: Exception) extends WsMsgSource

  sealed trait WsMsgServer extends WsMsgSource

  case object DecodeError extends WsMsgServer

  case object WsSuccess extends WsMsgServer
  final case class YourInfo(break:BreakState, config:BreakGameConfigImpl) extends WsMsgServer

  final case class UserJoinRoom(tankState:BreakState, override val frame: Long) extends  UserEvent with WsMsgServer
  final case class UserLeftRoom(playerId:String, name:String, breakId:Int, override val frame:Long) extends UserEvent with WsMsgServer
  final case class SyncGameState(state:GameContainerState) extends WsMsgServer
  final case class SyncGameAllState(gState:GameContainerAllState) extends WsMsgServer
  final case class Wrap(ws:Array[Byte]) extends WsMsgSource
  final case class PingPackage(sendTime:Long) extends WsMsgServer with WsMsgFront


  sealed trait GameEvent {
    val frame:Long
  }

  trait UserEvent extends GameEvent
  trait EnvironmentEvent extends GameEvent  //游戏环境产生事件
  trait UserActionEvent extends UserEvent{   //游戏用户动作事件
    val breakId:Int
    val serialNum:Byte
  }

  final case class UserMouseMove(breakId:Int, override val frame:Long, d:Float, override val serialNum:Byte) extends UserActionEvent

  final case class UC(breakId:Int,override val frame:Long,d:Float,override val serialNum:Byte) extends UserActionEvent with WsMsgFront with WsMsgServer
  type UserMouseClick = UC

  final case class GenerateBall(override val frame:Long, ball:BallState, s:Boolean) extends EnvironmentEvent with WsMsgServer


  /**生成砖块*/
  final case class GenerateObstacle(override val frame:Long,obstacleState: ObstacleState) extends EnvironmentEvent with WsMsgServer
  /**砖块消失事件*/
  final case class ObstacleRemove(obstacleId:Int, override val frame:Long) extends EnvironmentEvent with WsMsgServer

  /**游戏逻辑产生事件*/
  final case class ObstacleAttacked(obstacleId:Int, ballId:Int, damage:Int, override val frame:Long) extends EnvironmentEvent


}

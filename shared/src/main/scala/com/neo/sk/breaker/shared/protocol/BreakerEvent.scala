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
                                          f:Int,
                                          breakers:List[BreakState],
                                          balls:List[BallState],
                                          obstacle:List[ObstacleState],
                                          environment:List[ObstacleState],
                                        )

  final case class GameContainerState(
                                 f:Int,
                                 breakers:Option[List[BreakState]]
                               )

  /**前端建立WebSocket*/
  sealed trait WsMsgFrontSource
  case object CompleteMsgFrontServer extends WsMsgFrontSource
  case class FailMsgFrontServer(ex: Exception) extends WsMsgFrontSource

  sealed trait WsMsgFront extends WsMsgFrontSource

  case object StartGame extends WsMsgFront
  case object StopWebSocket extends WsMsgFront

  /**后台建立WebSocket*/
  trait WsMsgSource
  case object CompleteMsgServer extends WsMsgSource
  case class FailMsgServer(ex: Exception) extends WsMsgSource

  sealed trait WsMsgServer extends WsMsgSource

  case object DecodeError extends WsMsgServer

  case object WsSuccess extends WsMsgServer
  final case class YourInfo(break:BreakState, config:BreakGameConfigImpl) extends WsMsgServer

  final case class UserLeftRoom(playerId:String, name:String, breakId:Boolean, override val frame:Int) extends UserEvent with WsMsgServer
  final case class SyncGameState(state:GameContainerState) extends WsMsgServer
  final case class SyncGameAllState(gState:GameContainerAllState) extends WsMsgServer
  final case class Wrap(ws:Array[Byte]) extends WsMsgSource
  final case class PingPackage(sendTime:Long) extends WsMsgServer with WsMsgFront

  /**异地登录消息
    * WebSocket连接重新建立*/
  case object RebuildWebSocket extends WsMsgServer
  case object GameWaitOut extends WsMsgServer

  sealed trait GameEvent {
    val frame:Int
  }

  trait UserEvent extends GameEvent
  trait EnvironmentEvent extends GameEvent  //游戏环境产生事件
  trait UserActionEvent extends UserEvent{   //游戏用户动作事件
    val breakId:Boolean
    val serialNum:Byte
  }

  final case class UserMouseMove(breakId:Boolean, override val frame:Int, d:Float, override val serialNum:Byte) extends UserActionEvent

  final case class UC(breakId:Boolean,override val frame:Int,d:Float,override val serialNum:Byte) extends UserActionEvent with WsMsgFront with WsMsgServer
  type UserMouseClick = UC

  final case class UserPressKeyDown(breakId:Boolean,override val frame:Int,keyCodeDown:Byte,override val serialNum:Byte) extends UserActionEvent with WsMsgFront with WsMsgServer
  final case class UserPressKeyUp(breakId:Boolean,override val frame:Int,keyCodeUp:Byte,override val serialNum:Byte) extends UserActionEvent with WsMsgFront with WsMsgServer


  final case class Expression(breakId:Boolean, override val frame: Int,et:Byte,s:Option[String],override val serialNum:Byte) extends UserActionEvent with WsMsgFront with WsMsgServer

  /**生成小球*/
  final case class GenerateBall(override val frame:Int, ball:BallState, s:Boolean) extends EnvironmentEvent with WsMsgServer
  /**生成砖块*/
  final case class GenerateObstacle(override val frame:Int,obstacleState: ObstacleState) extends EnvironmentEvent with WsMsgServer
  /**砖块消失事件*/
  final case class ObstacleRemove(obstacleId:Int,breakId:Boolean,ballId:Int, override val frame:Int) extends EnvironmentEvent with WsMsgServer
  /**砖块移动事件*/
  final case class ObstacleMove(up:Boolean, override val frame:Int) extends EnvironmentEvent with WsMsgServer

  final case class GameOver(breakId:Boolean,override val frame:Int)extends EnvironmentEvent with WsMsgServer


  /**游戏逻辑产生事件*/
  final case class ObstacleAttacked(obstacleId:Int,breakId:Boolean, ballId:Int, damage:Int, override val frame:Int) extends EnvironmentEvent
  final case class BreakAttacked(breakId:Boolean, ballId:Int,override val frame:Int) extends EnvironmentEvent

}

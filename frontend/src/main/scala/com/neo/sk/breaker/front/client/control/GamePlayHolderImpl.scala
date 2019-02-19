package com.neo.sk.breaker.front.client.control

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.breaker.front.common.Routes
import com.neo.sk.breaker.front.pages.LoginPage
import com.neo.sk.breaker.front.utils.{JsFunc, Shortcut}
import com.neo.sk.breaker.shared.`object`.Breaker
import com.neo.sk.breaker.shared.game.GameContainerClientImpl
import com.neo.sk.breaker.shared.model.{Constants, Point}
import com.neo.sk.breaker.shared.model.Constants.GameState
import com.neo.sk.breaker.shared.protocol.BreakerEvent._
import com.neo.sk.breaker.shared.protocol.{BreakerEvent, UserProtocol}
import org.scalajs.dom
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.raw.MouseEvent

import scala.collection.mutable
import scala.xml.Elem

/**
  * User: sky
  * Date: 2018/10/29
  * Time: 13:00
  */
class GamePlayHolderImpl(name: String, playerInfo: UserProtocol.UserInfo) extends GameHolder(name) {
  private[this] val actionSerialNumGenerator = new AtomicInteger(0)
 private var lastMouseMoveAngle: Byte = 0
  private val perMouseMoveFrame = 3
  private var lastMoveFrame = -1L
  private val poKeyBoardMoveTheta = 2 * math.Pi / 72 //炮筒顺时针转
  private val neKeyBoardMoveTheta = -2 * math.Pi / 72 //炮筒逆时针转
  private var poKeyBoardFrame = 0L
  private var eKeyBoardState4AddBlood = true
  private val preExecuteFrameOffset = Constants.PreExecuteFrameOffset

  private val watchKeys = Set(
    KeyCode.Left,
    KeyCode.Up,
    KeyCode.Right,
    KeyCode.Down
  )

  private val gunAngleAdjust = Set(
    KeyCode.K,
    KeyCode.L
  )

  private val myKeySet = mutable.HashSet[Int]()

  private def changeKeys(k: Int): Int = k match {
    case KeyCode.W => KeyCode.Up
    case KeyCode.S => KeyCode.Down
    case KeyCode.A => KeyCode.Left
    case KeyCode.D => KeyCode.Right
    case origin => origin
  }

  def getActionSerialNum: Byte = (actionSerialNumGenerator.getAndIncrement()%127).toByte

  def start: Unit = {
    canvas.getCanvas.focus()
    dom.window.cancelAnimationFrame(nextFrame)
    Shortcut.cancelSchedule(timer)
    setGameState(GameState.loadingPlay)
    webSocketClient.setup(Routes.getJoinGameWebSocketUri(playerInfo))
    gameLoop()
  }

  private def addUserActionListenEvent(up:Boolean,breakPosition:Point): Unit = {
    canvas.getCanvas.focus()
    canvas.getCanvas.onmousemove = { e: dom.MouseEvent =>
      val point = Point(e.clientX.toFloat, e.clientY.toFloat)
      val theta = if(up) Point(canvasBoundary.*(canvasUnit).x/2,canvasBoundary.*(canvasUnit).y-breakPosition.y* canvasUnit).getTheta(point).toFloat else point.getTheta(Point(canvasBoundary.*(canvasUnit).x/2,breakPosition.y*canvasUnit)).toFloat
      if (gameContainerOpt.nonEmpty && gameState == GameState.play ) {
        val preMMFAction = BreakerEvent.UserMouseMove(gameContainerOpt.get.myBreakId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta,-1)
        gameContainerOpt.get.preExecuteUserEvent(preMMFAction)
      }
    }
    canvas.getCanvas.onclick = { e: MouseEvent =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val tank=gameContainerOpt.get.breakMap.get(gameContainerOpt.get.myBreakId)
        if(tank.nonEmpty&&tank.get.getBulletSize()>0){
          val point = Point(e.clientX.toFloat, e.clientY.toFloat)
          val theta = if(up) Point(canvasBoundary.*(canvasUnit).x/2,canvasBoundary.*(canvasUnit).y-breakPosition.y* canvasUnit).getTheta(point).toFloat else point.getTheta(Point(canvasBoundary.*(canvasUnit).x/2,breakPosition.y*canvasUnit)).toFloat
          val preExecuteAction = BreakerEvent.UC(gameContainerOpt.get.myBreakId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta, getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction) //发送鼠标位置
          e.preventDefault()
        }
      }
    }
  }

  override def gameOverCallback(): Unit = {
    setGameState(GameState.stop)
//    closeHolder
  }

  override protected def wsMessageHandler(data: WsMsgServer): Unit = {
    println(data.getClass)
    data match {
      case WsSuccess =>
        webSocketClient.sendMsg(BreakerEvent.StartGame)
        timer = Shortcut.schedule(gameLoop, 100)
        setGameState(GameState.loadingWait)

      case e: BreakerEvent.YourInfo =>
        val breaker=Breaker(e.config,e.break.playerId,e.break.breakId,e.break.name,e.break.position)
        println(s"new game the id is ${breaker.breakId}=====${breaker.name}")
        println(s"玩家信息${e}")
        Shortcut.cancelSchedule(timer)
        timer = Shortcut.schedule(gameLoop, e.config.frameDuration)
        /**
          * 更新游戏数据
          **/
        gameContainerOpt = Some(GameContainerClientImpl(e.config,drawFrame, ctx, breaker.playerId, breaker.breakId, breaker.name, canvasBoundary, canvasUnit, gameOverCallback))
        addUserActionListenEvent(breaker.up,breaker.getPosition)
        LoginPage.playerInfo=playerInfo.copy(nickName = breaker.name,playerId = Some(breaker.playerId))

      case e: BreakerEvent.SyncGameState =>
        gameContainerOpt.foreach(_.receiveGameContainerState(e.state))

      case e: BreakerEvent.SyncGameAllState =>
        gameContainerOpt.foreach(_.receiveGameContainerAllState(e.gState))
        dom.window.cancelAnimationFrame(nextFrame)
        nextFrame = dom.window.requestAnimationFrame(gameRender())
        setGameState(GameState.play)

      case e: UserActionEvent =>
        gameContainerOpt.foreach(_.receiveUserEvent(e))


      case e: GameEvent =>
        gameContainerOpt.foreach(_.receiveGameEvent(e))

      case e: PingPackage =>
        receivePingPackage(e)

      case _ => println(s"unknow msg={sss}")
    }
  }
}

package com.neo.sk.breaker.front.client.control

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.breaker.front.common.Routes
import com.neo.sk.breaker.front.pages.LoginPage
import com.neo.sk.breaker.front.utils.{JsFunc, Shortcut}
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

  private def addUserActionListenEvent(): Unit = {
    canvas.getCanvas.focus()
    canvas.getCanvas.onmousemove = { e: dom.MouseEvent =>
      val point = Point(e.clientX.toFloat, e.clientY.toFloat) + Point(24, 24)
      val theta = point.getTheta(canvasBoundary * canvasUnit / 2).toFloat
      if (gameContainerOpt.nonEmpty && gameState == GameState.play ) {
        val preMMFAction = BreakerEvent.UserMouseMove(gameContainerOpt.get.myBreakId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta,-1)
        gameContainerOpt.get.preExecuteUserEvent(preMMFAction)
      }
    }
    canvas.getCanvas.onclick = { e: MouseEvent =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val tank=gameContainerOpt.get.breakMap.get(gameContainerOpt.get.myBreakId)
        if(tank.nonEmpty&&tank.get.getBulletSize()>0){
          val point = Point(e.clientX.toFloat, e.clientY.toFloat) + Point(24, 24)
          val theta = point.getTheta(canvasBoundary * canvasUnit / 2).toFloat
          val preExecuteAction = BreakerEvent.UC(gameContainerOpt.get.myBreakId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta, getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction) //发送鼠标位置
          e.preventDefault()
        }
        //        audioForBullet.play()
      }
    }
  }

  override protected def wsMessageHandler(data: WsMsgServer): Unit = {
    println(data.getClass)
    data match {
      case WsSuccess =>
        webSocketClient.sendMsg(BreakerEvent.StartGame)

      case e: BreakerEvent.YourInfo =>
        println(s"new game the id is ${e.breakId}=====${e.name}")
        println(s"玩家信息${e}")
        timer = Shortcut.schedule(gameLoop, e.config.frameDuration)
        /**
          * 更新游戏数据
          **/
        gameContainerOpt = Some(GameContainerClientImpl(e.config,drawFrame, ctx, e.playerId, e.breakId, e.name, canvasBoundary, canvasUnit))
        LoginPage.playerInfo=playerInfo.copy(nickName = e.name,playerId = Some(e.playerId))

      case e: BreakerEvent.SyncGameState =>
        gameContainerOpt.foreach(_.receiveGameContainerState(e.state))

      case e: BreakerEvent.SyncGameAllState =>
        gameContainerOpt.foreach(_.receiveGameContainerAllState(e.gState))
        dom.window.cancelAnimationFrame(nextFrame)
        nextFrame = dom.window.requestAnimationFrame(gameRender())
        addUserActionListenEvent()
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

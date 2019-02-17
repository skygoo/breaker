package com.neo.sk.breaker.front.client.control

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.breaker.front.common.Routes
import com.neo.sk.breaker.front.components.StartGameModal
import com.neo.sk.breaker.front.utils.Shortcut
import com.neo.sk.breaker.shared.model.Constants
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
class GamePlayHolderImpl(name: String, playerInfoOpt: UserProtocol.UserInfo) extends GameHolder(name) {
  private[this] val actionSerialNumGenerator = new AtomicInteger(0)
 private var lastMouseMoveAngle: Byte = 0
  private val perMouseMoveFrame = 3
  private var lastMoveFrame = -1L
  private val poKeyBoardMoveTheta = 2 * math.Pi / 72 //炮筒顺时针转
  private val neKeyBoardMoveTheta = -2 * math.Pi / 72 //炮筒逆时针转
  private var poKeyBoardFrame = 0L
  private var eKeyBoardState4AddBlood = true
  private val preExecuteFrameOffset = Constants.PreExecuteFrameOffset
  private val startGameModal = new StartGameModal(gameStateVar, start, playerInfoOpt)

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

  def getStartGameModal(): Elem = {
    startGameModal.render
  }

  private def start(name: String, roomIdOpt: Option[Long]): Unit = {
    canvas.getCanvas.focus()
    dom.window.cancelAnimationFrame(nextFrame)
    Shortcut.cancelSchedule(timer)
    if (firstCome) {
      firstCome = false
      addUserActionListenEvent()
      setGameState(GameState.loadingPlay)
      webSocketClient.setup(Routes.getJoinGameWebSocketUri(playerInfoOpt, roomIdOpt))
      //      webSocketClient.sendMsg(TankGameEvent.StartGame(roomIdOpt,None))
      gameLoop()

    } else if (webSocketClient.getWsState) {
      gameContainerOpt match {
        case Some(gameContainer) =>
          gameContainerOpt.foreach(_.changeTankId(gameContainer.myTankId))
          if (Constants.supportLiveLimit) {
            webSocketClient.sendMsg(TankGameEvent.RestartGame(Some(gameContainer.myTankId), name))
          } else {
            webSocketClient.sendMsg(TankGameEvent.RestartGame(None, name))
          }

        case None =>
          webSocketClient.sendMsg(TankGameEvent.RestartGame(None, name))
      }
      setGameState(GameState.loadingPlay)
      gameLoop()

    } else {
      JsFunc.alert("网络连接失败，请重新刷新")
    }
  }

  private def addUserActionListenEvent(): Unit = {
    canvas.getCanvas.focus()
    canvas.getCanvas.onmousemove = { e: dom.MouseEvent =>
      val point = Point(e.clientX.toFloat, e.clientY.toFloat) + Point(24, 24)
      val theta = point.getTheta(canvasBoundary * canvasUnit / 2).toFloat
      val angle = point.getAngle(canvasBoundary * canvasUnit / 2)
      //remind tank自身流畅显示
      //fixme 此处序列号是否存疑
      val preMMFAction = TankGameEvent.UserMouseMove(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta,-1)
      gameContainerOpt.get.preExecuteUserEvent(preMMFAction)
      if (gameContainerOpt.nonEmpty && gameState == GameState.play && lastMoveFrame+perMouseMoveFrame < gameContainerOpt.get.systemFrame) {
        if (lastMouseMoveAngle!=angle) {
          lastMouseMoveAngle = angle
          lastMoveFrame = gameContainerOpt.get.systemFrame
          val preMMBAction = TankGameEvent.UM(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, angle, getActionSerialNum)
          sendMsg2Server(preMMBAction) //发送鼠标位置
          e.preventDefault()
        }
      }
    }
    canvas.getCanvas.onclick = { e: MouseEvent =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val tank=gameContainerOpt.get.tankMap.get(gameContainerOpt.get.myTankId)
        if(tank.nonEmpty&&tank.get.getBulletSize()>0){
          val point = Point(e.clientX.toFloat, e.clientY.toFloat) + Point(24, 24)
          val theta = point.getTheta(canvasBoundary * canvasUnit / 2).toFloat
          val preExecuteAction = TankGameEvent.UC(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta, getActionSerialNum)
          sendMsg2Server(preExecuteAction) //发送鼠标位置
          e.preventDefault()
        }
        //        audioForBullet.play()
      }
    }
  }

  override protected def setKillCallback(tank: Tank) = {
    if (gameContainerOpt.nonEmpty&&tank.tankId ==gameContainerOpt.get.tankId) {
      if (tank.lives <= 1) setGameState(GameState.stop)
    }
  }

  override protected def wsMessageHandler(data: WsMsgServer): Unit = {
    data match {
      case e: WsSuccess =>
        webSocketClient.sendMsg(BreakerEvent.StartGame)

      case e: TankGameEvent.YourInfo =>
        println(s"new game the id is ${e.tankId}=====${e.name}")
        println(s"玩家信息${e}")
        timer = Shortcut.schedule(gameLoop, e.config.frameDuration / e.config.playRate)
        //        audioForBgm.play()
        /**
          * 更新游戏数据
          **/
        gameContainerOpt = Some(GameContainerClientImpl(drawFrame, ctx, e.config, e.userId, e.tankId, e.name, canvasBoundary, canvasUnit, setKillCallback, versionInfoOpt))
        gameContainerOpt.get.changeTankId(e.tankId)
      //        gameContainerOpt.foreach(e =>)

      case e: TankGameEvent.YouAreKilled =>
        gameContainerOpt.foreach(_.updateDamageInfo(e.killTankNum, e.name, e.damageStatistics))

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

package com.neo.sk.breaker.front.client.control

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.breaker.front.common.Routes
import com.neo.sk.breaker.front.pages.LoginPage
import com.neo.sk.breaker.front.utils.{JsFunc, Shortcut}
import com.neo.sk.breaker.shared.`object`.Breaker
import com.neo.sk.breaker.shared.game.GameContainerClientImpl
import com.neo.sk.breaker.shared.model.{Constants, Point}
import com.neo.sk.breaker.shared.model.Constants.{GameState, RoomType}
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
case class GamePlayHolderImpl(name: String, playerInfo: UserProtocol.UserInfo) extends GameHolder(name) {
  private[this] val actionSerialNumGenerator = new AtomicInteger(0)
  private val preExecuteFrameOffset = Constants.PreExecuteFrameOffset

  var spaceKeyUpState = false
  var enterKeyUpState = false

  private val watchKeys = Set(
    KeyCode.Left,
    KeyCode.Right,
    KeyCode.A,
    KeyCode.D
  )

  private val myKeySet = mutable.HashSet[Int]()

  private def changeKeys(up: Boolean, k: Int): Int =
    if (up) {
      k match {
        case KeyCode.Left => KeyCode.Right
        case KeyCode.Right => KeyCode.Left
        case KeyCode.A => KeyCode.Right
        case KeyCode.D => KeyCode.Left
        case _ => k
      }
    } else {
      k match {
        case KeyCode.A => KeyCode.Left
        case KeyCode.D => KeyCode.Right
        case _ => k
      }
    }

  def getActionSerialNum: Byte = (actionSerialNumGenerator.getAndIncrement() % 127).toByte

  def start: Unit = {
    canvas.getCanvas.focus()
    dom.window.cancelAnimationFrame(nextFrame)
    Shortcut.cancelSchedule(timer)
    setGameState(GameState.loadingPlay)
    webSocketClient.setup(Routes.getJoinGameWebSocketUri(playerInfo))
    gameLoop()
  }

  def sendExpression(et: Byte, s: Option[String]) = {
    canvas.getCanvas.focus()
    val event = BreakerEvent.Expression(gameContainerOpt.get.myBreakId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, et, s, getActionSerialNum)
    gameContainerOpt.get.preExecuteUserEvent(event)
    sendMsg2Server(event)
  }

  private def addUserActionListenEvent(up: Boolean, boundary: Point): Unit = {
    canvas.getCanvas.focus()
    canvas.getCanvas.onmousemove = { e: dom.MouseEvent =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val point = Point(e.clientX.toFloat, e.clientY.toFloat)
        val break = gameContainerOpt.get.breakMap.get(gameContainerOpt.get.myBreakId)
        if (break.nonEmpty) {
          val theta = if (up) ((Point((canvasBoundary.x + boundary.x) / 2, canvasBoundary.y) - break.get.getPosition) * canvasUnit).getTheta(point).toFloat else point.getTheta((break.get.getPosition + Point((canvasBoundary.x - boundary.x) / 2, 0)) * canvasUnit).toFloat
          val preMMFAction = BreakerEvent.UserMouseMove(gameContainerOpt.get.myBreakId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta, -1)
          gameContainerOpt.get.preExecuteUserEvent(preMMFAction)
        }
      }
    }
    canvas.getCanvas.onclick = { e: MouseEvent =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val break = gameContainerOpt.get.breakMap.get(gameContainerOpt.get.myBreakId)
        if (break.nonEmpty && break.get.getBulletPercent() > 0.9) {
          val point = Point(e.clientX.toFloat, e.clientY.toFloat)
          val theta = if (up) ((Point((canvasBoundary.x + boundary.x) / 2, canvasBoundary.y) - break.get.getPosition) * canvasUnit).getTheta(point).toFloat else point.getTheta((break.get.getPosition + Point((canvasBoundary.x - boundary.x) / 2, 0)) * canvasUnit).toFloat
          val preExecuteAction = BreakerEvent.UC(gameContainerOpt.get.myBreakId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta, getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction) //发送鼠标位置
          e.preventDefault()
        }
      }
    }
    canvas.getCanvas.onkeydown = { e: dom.KeyboardEvent =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val keyCode = changeKeys(up, e.keyCode)
        if (watchKeys.contains(keyCode) && !myKeySet.contains(keyCode)) {
          myKeySet.add(keyCode)
          val preExecuteAction = BreakerEvent.UserPressKeyDown(gameContainerOpt.get.myBreakId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode.toByte, getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction)
          e.preventDefault()
        }
      }
    }
    canvas.getCanvas.onkeyup = { e: dom.KeyboardEvent =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val keyCode = changeKeys(up, e.keyCode)
        if (watchKeys.contains(keyCode)) {
          myKeySet.remove(keyCode)
          val preExecuteAction = BreakerEvent.UserPressKeyUp(gameContainerOpt.get.myBreakId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode.toByte, getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction)
          e.preventDefault()

        }
      }
    }
  }

  private def addUserComeBackListenEvent() = {
    canvas.getCanvas.focus()
    canvas.getCanvas.onkeydown = { e: dom.KeyboardEvent =>
      if (gameState == GameState.stop) {
        val keyCode = e.keyCode
        if (keyCode == KeyCode.Space && spaceKeyUpState) {
          //          audioForBullet.play()
          spaceKeyUpState = false
          println("reStart------!")
          val preExecuteAction = BreakerEvent.StartGame
          sendMsg2Server(preExecuteAction) //发送鼠标位置
          Shortcut.cancelSchedule(timer)
          timer = Shortcut.schedule(gameLoop, 100)
          setGameState(GameState.loadingWait)
          e.preventDefault()
        }
        if (keyCode == KeyCode.Enter && enterKeyUpState) {
          //          audioForBullet.play()
          enterKeyUpState = false
          val preExecuteAction = BreakerEvent.StopWebSocket
          sendMsg2Server(preExecuteAction) //发送鼠标位置
          closeHolder
          Shortcut.redirect("")
          e.preventDefault()
        }
      }
    }
  }

  override def gameOverCallback(): Unit = {
    spaceKeyUpState = true
    enterKeyUpState = true
    setGameState(GameState.stop)
    Shortcut.cancelSchedule(timer)
    addUserComeBackListenEvent()
  }

  override protected def wsMessageHandler(data: WsMsgServer): Unit = {
    println(data.getClass)
    data match {
      case WsSuccess =>
        webSocketClient.sendMsg(BreakerEvent.StartGame)
        Shortcut.cancelSchedule(timer)
        timer = Shortcut.schedule(gameLoop, 100)
        setGameState(GameState.loadingWait)
        decTime(60)

      case e: BreakerEvent.YourInfo =>

        val breaker = new Breaker(e.config, e.break)
        println(s"new game the id is ${breaker.breakId}=====${breaker.name}")
        println(s"玩家信息${e}")
        Shortcut.cancelSchedule(timer)
        timer = Shortcut.schedule(gameLoop, e.config.frameDuration)

        /**
          * 更新游戏数据
          **/
        gameContainerOpt = Some(GameContainerClientImpl(e.config, drawFrame, ctx, breaker.playerId, breaker.breakId, breaker.name, canvasBoundary, canvasUnit, gameOverCallback))
        addUserActionListenEvent(breaker.up, e.config.boundary)
        LoginPage.playerInfo = playerInfo.copy(nickName = breaker.name, playerId = Some(breaker.playerId))

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

      case BreakerEvent.RebuildWebSocket =>
        drawInfoMsg("存在异地登录。。")
        closeHolder

      case BreakerEvent.GameWaitOut=>
        drawInfoMsg("等待超时")
        closeHolder
        Shortcut.scheduleOnce(()=>Shortcut.redirect(""),1500)

      case e: PingPackage =>
        receivePingPackage(e)

      case _ => println(s"unknow msg={sss}")
    }
  }
}

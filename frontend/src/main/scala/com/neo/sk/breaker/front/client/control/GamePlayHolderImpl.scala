package com.neo.sk.breaker.front.client.control

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.breaker.front.common.Routes
import com.neo.sk.breaker.front.components.StartGameModal
import com.neo.sk.breaker.front.model.PlayerInfo
import com.neo.sk.breaker.front.utils.{JsFunc, Shortcut}
import com.neo.sk.breaker.shared.`object`.breaker
import com.neo.sk.breaker.shared.game.GameContainerClientImpl
import com.neo.sk.breaker.shared.model.Constants.GameState
import com.neo.sk.breaker.shared.model.Point
import com.neo.sk.breaker.shared.protocol.breakerGameEvent
import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.raw.MouseEvent

import scala.collection.mutable
import scala.xml.Elem

/**
  * User: sky
  * Date: 2018/10/29
  * Time: 13:00
  */
class GamePlayHolderImpl(name: String, playerInfoOpt: Option[PlayerInfo] = None) extends GameHolder(name) {
  private[this] val actionSerialNumGenerator = new AtomicInteger(0)
  private var spaceKeyUpState = true
  private var lastMouseMoveAngle: Byte = 0
  private val perMouseMoveFrame = 3
  private var lastMoveFrame = -1L
  private val poKeyBoardMoveTheta = 2 * math.Pi / 72 //炮筒顺时针转
  private val neKeyBoardMoveTheta = -2 * math.Pi / 72 //炮筒逆时针转
  private var poKeyBoardFrame = 0L
  private var eKeyBoardState4AddBlood = true
  private val preExecuteFrameOffset = com.neo.sk.breaker.shared.model.Constants.PreExecuteFrameOffset
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
      webSocketClient.setup(Routes.getJoinGameWebSocketUri(name, playerInfoOpt, roomIdOpt))
      //      webSocketClient.sendMsg(breakerGameEvent.StartGame(roomIdOpt,None))
      gameLoop()

    } else if (webSocketClient.getWsState) {
      gameContainerOpt match {
        case Some(gameContainer) =>
          gameContainerOpt.foreach(_.changebreakerId(gameContainer.mybreakerId))
          if (Constants.supportLiveLimit) {
            webSocketClient.sendMsg(breakerGameEvent.RestartGame(Some(gameContainer.mybreakerId), name))
          } else {
            webSocketClient.sendMsg(breakerGameEvent.RestartGame(None, name))
          }

        case None =>
          webSocketClient.sendMsg(breakerGameEvent.RestartGame(None, name))
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
      //remind breaker自身流畅显示
      //fixme 此处序列号是否存疑
      val preMMFAction = breakerGameEvent.UserMouseMove(gameContainerOpt.get.mybreakerId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta,-1)
      gameContainerOpt.get.preExecuteUserEvent(preMMFAction)
      if (gameContainerOpt.nonEmpty && gameState == GameState.play && lastMoveFrame+perMouseMoveFrame < gameContainerOpt.get.systemFrame) {
        if (lastMouseMoveAngle!=angle) {
          lastMouseMoveAngle = angle
          lastMoveFrame = gameContainerOpt.get.systemFrame
          val preMMBAction = breakerGameEvent.UM(gameContainerOpt.get.mybreakerId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, angle, getActionSerialNum)
          sendMsg2Server(preMMBAction) //发送鼠标位置
          e.preventDefault()
        }
      }
    }
    canvas.getCanvas.onclick = { e: MouseEvent =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val breaker=gameContainerOpt.get.breakerMap.get(gameContainerOpt.get.mybreakerId)
        if(breaker.nonEmpty&&breaker.get.getBulletSize()>0){
          val point = Point(e.clientX.toFloat, e.clientY.toFloat) + Point(24, 24)
          val theta = point.getTheta(canvasBoundary * canvasUnit / 2).toFloat
          val preExecuteAction = breakerGameEvent.UC(gameContainerOpt.get.mybreakerId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta, getActionSerialNum)
          sendMsg2Server(preExecuteAction) //发送鼠标位置
          e.preventDefault()
        }
        //        audioForBullet.play()
      }
    }

    canvas.getCanvas.onkeydown = { e: dom.KeyboardEvent =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        /**
          * 增加按键操作
          **/
        val keyCode = changeKeys(e.keyCode)
        if (watchKeys.contains(keyCode) && !myKeySet.contains(keyCode)) {
          myKeySet.add(keyCode)
          //          println(s"key down: [${e.keyCode}]")
          val preExecuteAction = breakerGameEvent.UserPressKeyDown(gameContainerOpt.get.mybreakerId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode.toByte, getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction)
          if (com.neo.sk.breaker.shared.model.Constants.fakeRender) {
            gameContainerOpt.get.addMyAction(preExecuteAction)
          }
          e.preventDefault()
        }
        if (gunAngleAdjust.contains(keyCode) && poKeyBoardFrame != gameContainerOpt.get.systemFrame) {
          myKeySet.remove(keyCode)
          //          println(s"key down: [${e.keyCode}]")
          poKeyBoardFrame = gameContainerOpt.get.systemFrame
          val Theta =
            if (keyCode == KeyCode.K) poKeyBoardMoveTheta
            else neKeyBoardMoveTheta
          val preExecuteAction = breakerGameEvent.UserKeyboardMove(gameContainerOpt.get.mybreakerId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, Theta.toFloat, getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction)
          e.preventDefault()
        }
        else if (keyCode == KeyCode.Space && spaceKeyUpState) {
          //          audioForBullet.play()
          spaceKeyUpState = false
          val preExecuteAction = breakerGameEvent.UC(gameContainerOpt.get.mybreakerId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, gameContainerOpt.get.breakerMap(gameContainerOpt.get.mybreakerId).getGunDirection(), getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction) //发送鼠标位置
          e.preventDefault()
        }
        else if (keyCode == KeyCode.E) {
          /**
            * 吃道具
            **/
          eKeyBoardState4AddBlood = false
          val preExecuteAction = breakerGameEvent.UserPressKeyMedical(gameContainerOpt.get.mybreakerId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, serialNum = getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction)
          e.preventDefault()
        }
      }

    }

    canvas.getCanvas.onkeyup = { e: dom.KeyboardEvent =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val keyCode = changeKeys(e.keyCode)
        if (watchKeys.contains(keyCode)) {
          myKeySet.remove(keyCode)
          //          println(s"key up: [${e.keyCode}]")
          val preExecuteAction = breakerGameEvent.UserPressKeyUp(gameContainerOpt.get.mybreakerId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode.toByte, getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction)
          if (com.neo.sk.breaker.shared.model.Constants.fakeRender) {
            gameContainerOpt.get.addMyAction(preExecuteAction)
          }
          e.preventDefault()

        }
        //        if (gunAngleAdjust.contains(keyCode)) {
        //          myKeySet.remove(keyCode)
        //          println(s"key up: [${e.keyCode}]")
        //
        //          val Theta = if(keyCode == KeyCode.K){
        //             poKeyBoardMoveTheta
        //          }
        //          else {
        //            neKeyBoardMoveTheta
        //          }
        //          val preExecuteAction = breakerGameEvent.UserKeyboardMove(gameContainerOpt.get.mybreakerId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset,Theta.toFloat , getActionSerialNum)
        //          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
        //          sendMsg2Server(preExecuteAction)
        //          e.preventDefault()

        //        }
        else if (e.keyCode == KeyCode.Space) {
          spaceKeyUpState = true
          e.preventDefault()
        }
        else if (e.keyCode == KeyCode.E) {
          eKeyBoardState4AddBlood = true
          e.preventDefault()
        }
      }
    }
  }

  override protected def setKillCallback(breaker: breaker) = {
    if (gameContainerOpt.nonEmpty&&breaker.breakerId ==gameContainerOpt.get.breakerId) {
      if (breaker.lives <= 1) setGameState(GameState.stop)
    }
  }

  override protected def wsMessageHandler(data: breakerGameEvent.WsMsgServer): Unit = {
    data match {
      case e: breakerGameEvent.WsSuccess =>
        webSocketClient.sendMsg(breakerGameEvent.StartGame(e.roomId, None))

      case e: breakerGameEvent.YourInfo =>
        println(s"new game the id is ${e.breakerId}=====${e.name}")
        println(s"玩家信息${e}")
        timer = Shortcut.schedule(gameLoop, e.config.frameDuration / e.config.playRate)
        //        audioForBgm.play()
        /**
          * 更新游戏数据
          **/
        gameContainerOpt = Some(GameContainerClientImpl(drawFrame, ctx, e.config, e.userId, e.breakerId, e.name, canvasBoundary, canvasUnit, setKillCallback, versionInfoOpt))
        gameContainerOpt.get.changebreakerId(e.breakerId)
      //        gameContainerOpt.foreach(e =>)

      case e: breakerGameEvent.breakerFollowEventSnap =>
        println(s"game breakerFollowEventSnap =${e} systemFrame=${gameContainerOpt.get.systemFrame} breakerId=${gameContainerOpt.get.mybreakerId} ")
        gameContainerOpt.foreach(_.receivebreakerFollowEventSnap(e))

      case e: breakerGameEvent.YouAreKilled =>

        /**
          * 死亡重玩
          **/
        gameContainerOpt.foreach(_.updateDamageInfo(e.killbreakerNum, e.name, e.damageStatistics))
        //        dom.window.cancelAnimationFrame(nextFrame)
        //        gameContainerOpt.foreach(_.drawGameStop())
        if ((Constants.supportLiveLimit && !e.hasLife) || (!Constants.supportLiveLimit)) {
          setGameState(GameState.stop)
          gameContainerOpt.foreach(_.changebreakerId(e.breakerId))
          //          audioForBgm.pause()
          //          audioForDead.play()
        }

      case e: breakerGameEvent.SyncGameState =>
        gameContainerOpt.foreach(_.receiveGameContainerState(e.state))

      case e: breakerGameEvent.SyncGameAllState =>
        gameContainerOpt.foreach(_.receiveGameContainerAllState(e.gState))
        dom.window.cancelAnimationFrame(nextFrame)
        nextFrame = dom.window.requestAnimationFrame(gameRender())
        setGameState(GameState.play)

      case e: breakerGameEvent.UserActionEvent =>
        e match {
          case e:breakerGameEvent.UM=>
            if(gameContainerOpt.nonEmpty){
              if(gameContainerOpt.get.mybreakerId!=e.breakerId){
                gameContainerOpt.foreach(_.receiveUserEvent(e))
              }
            }
          case _=>
            gameContainerOpt.foreach(_.receiveUserEvent(e))
        }


      case e: breakerGameEvent.GameEvent =>
        gameContainerOpt.foreach(_.receiveGameEvent(e))
        e match {
          case e: breakerGameEvent.UserRelive =>
            if (e.userId == gameContainerOpt.get.myId) {
              dom.window.cancelAnimationFrame(nextFrame)
              nextFrame = dom.window.requestAnimationFrame(gameRender())
            }
          case _ =>
        }

      case e: breakerGameEvent.PingPackage =>
        receivePingPackage(e)

      case breakerGameEvent.RebuildWebSocket =>
        gameContainerOpt.foreach(_.drawReplayMsg("存在异地登录。。"))
        closeHolder

      case _ => println(s"unknow msg={sss}")
    }
  }
}

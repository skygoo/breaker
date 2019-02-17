
package com.neo.sk.breaker.front.client.control

import com.neo.sk.breaker.front.client.{NetworkInfo, WebSocketClient}
import com.neo.sk.breaker.front.utils.{JsFunc, Shortcut}
import com.neo.sk.breaker.front.utils.canvas.MiddleFrameInJs
import com.neo.sk.breaker.shared.game.GameContainerClientImpl
import com.neo.sk.breaker.shared.model.Constants.GameState
import com.neo.sk.breaker.shared.model.{Constants, Point}
import com.neo.sk.breaker.shared.protocol.BreakerEvent
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.html.Script
import org.scalajs.dom.raw.{Event, VisibilityState}

/**
  * User: sky
  * Date: 2018/10/29
  * Time: 12:47
  * 需要构造参数，所以重构为抽象类
  */
abstract class GameHolder(name: String) extends NetworkInfo {
  val drawFrame = new MiddleFrameInJs
  protected var canvasWidth = dom.window.innerWidth.toFloat
  protected var canvasHeight = dom.window.innerHeight.toFloat

  protected val canvas = drawFrame.createCanvas(name, canvasWidth, canvasHeight)
  protected val ctx = canvas.getCtx


  protected var canvasUnit = getCanvasUnit(canvasWidth)
  protected var canvasBoundary = Point(canvasWidth, canvasHeight) / canvasUnit

  println(s"test111111111111=${canvasUnit},=${canvasWidth}")

  var versionInfoOpt:Option[String]=None
  val versionScript = dom.document.getElementById("js-version")
  try {
    versionScript match {
      case script: Script =>
        versionInfoOpt=Some(script.src.split("id=")(1))
      case _ =>
    }
  }catch {case exception: Exception=>
      println(exception.getCause)
  }

  protected val gameStateVar: Var[Int] = Var(GameState.firstCome)
  protected var gameState: Int = GameState.firstCome

  //  protected var killerName:String = ""


  protected var gameContainerOpt: Option[GameContainerClientImpl] = None

  protected val webSocketClient: WebSocketClient = WebSocketClient(wsConnectSuccess, wsConnectError, wsMessageHandler, wsConnectClose, setDateSize )


  protected var timer: Int = 0
  //  protected var reStartTimer:Int = 0
  /**
    * 倒计时，config
    **/
  protected val reStartInterval = 1000
  protected val countDown = 3
  protected var countDownTimes = countDown
  protected var nextFrame = 0
  protected var logicFrameTime = System.currentTimeMillis()

  //fixme 此处打印渲染时间
  /*private var renderTime:Long = 0
  private var renderTimes = 0

  Shortcut.schedule( () =>{
    if(renderTimes != 0){
      println(s"render page use avg time:${renderTime / renderTimes}ms")
    }else{
      println(s"render page use avg time:0 ms")
    }
    renderTime = 0
    renderTimes = 0
  }, 5000L)*/

  private def onVisibilityChanged = { e: Event =>
    if (dom.document.visibilityState == VisibilityState.visible) {
      println("change tab into current")
      onCurTabEventCallback
    } else {
      println("has change tab")
    }
  }

  protected def onCurTabEventCallback={
//    webSocketClient.sendMsg(TankGameEvent.GetSyncGameState)
  }

  dom.window.addEventListener("visibilitychange", onVisibilityChanged, false)

  def closeHolder = {
    dom.window.cancelAnimationFrame(nextFrame)
    Shortcut.cancelSchedule(timer)
    webSocketClient.closeWs
  }

  protected def gameRender(): Double => Unit = { d =>
    val curTime = System.currentTimeMillis()
    val offsetTime = curTime - logicFrameTime
    drawGame(offsetTime)
    if(gameState == GameState.stop) gameContainerOpt.foreach(_.drawCombatGains())
    nextFrame = dom.window.requestAnimationFrame(gameRender())
  }


  protected def setGameState(s: Int): Unit = {
    gameStateVar := s
    gameState = s
  }

  protected def sendMsg2Server(msg: BreakerEvent.WsMsgFront): Unit = {
    if (gameState == GameState.play)
      webSocketClient.sendMsg(msg)
  }

  protected def checkScreenSize = {
    val newWidth = dom.window.innerWidth.toFloat
    val newHeight = dom.window.innerHeight.toFloat
    if (newWidth != canvasWidth || newHeight != canvasHeight) {
      println("the screen size is change")
      canvasWidth = newWidth
      canvasHeight = newHeight
      canvasUnit = getCanvasUnit(canvasWidth)
      canvasBoundary = Point(canvasWidth, canvasHeight) / canvasUnit
      println(s"update screen=$canvasUnit,=${(canvasWidth, canvasHeight)}")
      canvas.setWidth(canvasWidth.toInt)
      canvas.setHeight(canvasHeight.toInt)
      gameContainerOpt.foreach { r =>
        r.updateClientSize(canvasBoundary, canvasUnit)
      }
    }
  }

  protected def gameLoop(): Unit = {
    checkScreenSize
    gameState match {
      case GameState.loadingPlay =>
        println(s"等待同步数据")
        gameContainerOpt.foreach(_.drawGameLoading())
      case GameState.play =>
        gameContainerOpt.foreach(_.update())
        logicFrameTime = System.currentTimeMillis()
        ping()

      case GameState.stop =>
        //remind 此处需要注意
        dom.document.getElementById("input_mask_id").asInstanceOf[dom.html.Div].focus()
        gameContainerOpt.foreach{r =>r.update()}
        logicFrameTime = System.currentTimeMillis()
        ping()

      case _ => println(s"state=$gameState failed")
    }
  }

//  private def drawGame(offsetTime: Long) = {
//    gameContainerOpt.foreach(_.drawGame(offsetTime, getNetworkLatency, dateSize))
  private def drawGame(offsetTime: Long) = {
    gameContainerOpt.foreach(_.drawGame(offsetTime, getNetworkLatency))
  }


  //  protected def drawGameRestart()

  protected def wsConnectSuccess(e: Event) = {
    println(s"连接服务器成功")
    e
  }

  protected def wsConnectError(e: Event) = {
    JsFunc.alert("网络连接失败，请重新刷新")
    e
  }

  protected def wsConnectClose(e: Event) = {
    JsFunc.alert("网络连接失败，请重新刷新")
    e
  }

  protected def wsMessageHandler(data: BreakerEvent.WsMsgServer)


  protected def getCanvasUnit(canvasWidth: Float): Int = /*(canvasWidth / Constants.WindowView.x).toInt*/ 3
}

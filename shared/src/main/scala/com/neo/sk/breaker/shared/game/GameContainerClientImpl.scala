package com.neo.sk.breaker.shared.game

import com.neo.sk.breaker.shared.`object`.{Ball, Breaker, Obstacle}
import com.neo.sk.breaker.shared.game.config.BreakGameConfig
import com.neo.sk.breaker.shared.game.view._
import com.neo.sk.breaker.shared.model.Constants.GameAnimation
import com.neo.sk.breaker.shared.model.Point
import com.neo.sk.breaker.shared.protocol.BreakerEvent._
import com.neo.sk.breaker.shared.util.canvas.{MiddleContext, MiddleFrame}

import scala.collection.mutable

/**
  * Created by sky
  * Date on 2019/2/14
  * Time at 下午6:31
  */
case class GameContainerClientImpl(
                                    config: BreakGameConfig,
                                    drawFrame: MiddleFrame,
                                    ctx: MiddleContext,
                                    myId: String,
                                    var myBreakId: Int,
                                    myName: String,
                                    var canvasSize: Point,
                                    var canvasUnit: Float,
                                  ) extends GameContainer with EsRecover
  with BackDrawUtil
  with InfoDrawUtil
  with FpsComponentsDrawUtil
  with ObstacleDrawUtil
  with BallDrawUtil
  with BreakerDrawUtil {
  def debug(msg: String) = println("debug:" + msg)

  def info(msg: String): Unit = println("info:" + msg)

  val Pi = 3.14f

  protected val obstacleAttackedAnimationMap = mutable.HashMap[Int, Int]()
  private var gameContainerAllStateOpt: Option[GameContainerAllState] = None
  private var gameContainerStateOpt: Option[GameContainerState] = None
  protected var waitSyncData: Boolean = true

  private val uncheckedActionMap = mutable.HashMap[Byte, Long]() //serinum -> frame


  def updateClientSize(canvasS: Point, cUnit: Float) = {
    canvasUnit = cUnit
    canvasSize = canvasS
  }

  protected def handleGameContainerAllState(gameContainerAllState: GameContainerAllState) = {
    systemFrame = gameContainerAllState.f
    quadTree.clear()
    breakMap.clear()
    obstacleMap.clear()
    ballMap.clear()

    gameContainerAllState.breakers.foreach { t =>
      val breaker = new Breaker(config,t)
      quadTree.insert(breaker)
      breakMap.put(t.breakId, breaker)
    }
    gameContainerAllState.obstacle.foreach { o =>
      val obstacle = Obstacle(config, o)
      quadTree.insert(obstacle)
      obstacleMap.put(o.oId, obstacle)
    }
    gameContainerAllState.balls.foreach { t =>
      val bullet = new Ball(config, t)
      quadTree.insert(bullet)
      ballMap.put(t.bId, bullet)
    }
    gameContainerAllState.environment.foreach { t =>
      val obstacle = Obstacle(config, t)
      quadTree.insert(obstacle)
      environmentMap.put(obstacle.oId, obstacle)
    }
    waitSyncData = false
  }

  protected def handleGameContainerState(gameContainerState: GameContainerState) = {
    val curFrame = systemFrame
    val startTime = System.currentTimeMillis()
    (curFrame until gameContainerState.f).foreach { _ =>
      super.update()
      if (config.esRecoverSupport) addGameSnapShot(systemFrame, getGameContainerAllState())
    }
    val endTime = System.currentTimeMillis()
    if (curFrame < gameContainerState.f) {
      println(s"handleGameContainerState update to now use Time=${endTime - startTime} and systemFrame=${systemFrame} sysFrame=${gameContainerState.f}")
    }
  }

  def receiveGameContainerAllState(gameContainerAllState: GameContainerAllState) = {
    println(gameContainerAllState)
    gameContainerAllStateOpt = Some(gameContainerAllState)
  }

  def receiveGameContainerState(gameContainerState: GameContainerState) = {
    if (gameContainerState.f != systemFrame) {
      gameContainerStateOpt = Some(gameContainerState)
    }
  }

  def removePreEvent(frame:Long, breakId:Int, serialNum:Byte):Unit = {
    actionEventMap.get(frame).foreach{ actions =>
      actionEventMap.put(frame,actions.filterNot(t => t.breakId == breakId && t.serialNum == serialNum))
    }
  }

  def receiveGameEvent(e: GameEvent) = {
    if (e.frame >= systemFrame) {
      addGameEvent(e)
    } else if (config.esRecoverSupport) {
      println(s"rollback-frame=${e.frame},curFrame=${this.systemFrame},e=${e}")
      rollback4GameEvent(e)
    }
  }

  def receiveUserEvent(e: UserActionEvent) = {
    if (e.breakId == myBreakId) {
      uncheckedActionMap.get(e.serialNum) match {
        case Some(preFrame) =>
          if (e.frame != preFrame) {
            println(s"preFrame=$preFrame eventFrame=${e.frame} curFrame=$systemFrame")
            if (preFrame < e.frame && config.esRecoverSupport) {
              if (preFrame >= systemFrame) {
                removePreEvent(preFrame, e.breakId, e.serialNum)
                addUserAction(e)
              } else if (e.frame >= systemFrame) {
                removePreEventHistory(preFrame, e.breakId, e.serialNum)
                rollback(preFrame)
                addUserAction(e)
              } else {
                removePreEventHistory(preFrame, e.breakId, e.serialNum)
                addUserActionHistory(e)
                rollback(preFrame)
              }
            }
          }
        case None =>
          if (e.frame >= systemFrame) {
            addUserAction(e)
          } else if (config.esRecoverSupport) {
            rollback4UserActionEvent(e)
          }
      }
    } else {
      if (e.frame >= systemFrame) {
        addUserAction(e)
      } else if (config.esRecoverSupport) {
        rollback4UserActionEvent(e)
      }
    }
  }

  override protected def handleObstacleAttacked(e: ObstacleAttacked): Unit = {
    super.handleObstacleAttacked(e)
    if (obstacleMap.get(e.obstacleId).nonEmpty || environmentMap.get(e.obstacleId).nonEmpty) {
      obstacleAttackedAnimationMap.put(e.obstacleId, GameAnimation.bulletHitAnimationFrame)
    }
  }

  override def tankExecuteLaunchBulletAction(breaker: Breaker): Unit = {
    breaker.launchBullet()
  }

  override protected def clearEventWhenUpdate(): Unit = {
    if (config.esRecoverSupport) {
      addEventHistory(systemFrame, gameEventMap.getOrElse(systemFrame, Nil), actionEventMap.getOrElse(systemFrame, Nil))
    }
    gameEventMap -= systemFrame
    actionEventMap -= systemFrame
    systemFrame += 1
  }

  def preExecuteUserEvent(action: UserActionEvent) = {
    addUserAction(action)
    uncheckedActionMap.put(action.serialNum, action.frame)
  }

  protected def addGameEvents(frame:Long,events:List[GameEvent],actionEvents:List[UserActionEvent]) = {
    gameEventMap.put(frame,events)
    actionEventMap.put(frame,actionEvents)
  }

  override def update(): Unit = {
    //    val startTime = System.currentTimeMillis()
    if (gameContainerAllStateOpt.nonEmpty) {
      val gameContainerAllState = gameContainerAllStateOpt.get
      info(s"立即同步所有数据，curSystemFrame=${systemFrame},sync game container state frame=${gameContainerAllState.f}")
      handleGameContainerAllState(gameContainerAllState)
      gameContainerAllStateOpt = None
      if (config.esRecoverSupport) {
        clearEsRecoverData()
        addGameSnapShot(systemFrame, this.getGameContainerAllState())
      }
    } else if (gameContainerStateOpt.nonEmpty) {
      info(s"同步数据，curSystemFrame=${systemFrame},sync game container state frame=${gameContainerStateOpt.get.f}")
      handleGameContainerState(gameContainerStateOpt.get)
      gameContainerStateOpt = None
      if (config.esRecoverSupport) {
        clearEsRecoverData()
        addGameSnapShot(systemFrame, this.getGameContainerAllState())
      }
    } else {
      super.update()
      if (config.esRecoverSupport) addGameSnapShot(systemFrame, getGameContainerAllState())
    }
  }

  protected def rollbackUpdate(): Unit = {
    super.update()
    if (config.esRecoverSupport) addGameSnapShot(systemFrame, getGameContainerAllState())
  }

  def drawGame(time: Long, networkLatency: Long): Unit = {
    val offsetTime = math.min(time, config.frameDuration)
    val h = canvasSize.y
    val w = canvasSize.x
    //    val startTime = System.currentTimeMillis()
    if (!waitSyncData) {
      ctx.setLineCap("round")
      ctx.setLineJoin("round")
      breakMap.get(myBreakId) match {
        case Some(break) =>
          val offset = if(break.up) Point((w+boundary.x)/2,h) else Point((w-boundary.x)/2,0)
          drawBackground()
          drawObstacles(break.up,offset)
          drawEnvironment(break.up,offset)
          drawBalls(break.up,offset,offsetTime)
          drawBreaker(break.up,offset,offsetTime)
          renderFps(networkLatency)
          val endTime = System.currentTimeMillis()
        //          renderTimes += 1
        //          renderTime += endTime - startTime


        case None =>
        //          info(s"tankid=${myTankId} has no in tankMap.....................................")
        //          setGameState(GameState.stop)
        //          if(isObserve) drawDeadImg()
      }
    }
  }

}

package com.neo.sk.breaker.shared.game

import com.neo.sk.breaker.shared.game.config.BreakGameConfig
import com.neo.sk.breaker.shared.game.view.{BackDrawUtil, InfoDrawUtil}
import com.neo.sk.breaker.shared.model.Point
import com.neo.sk.breaker.shared.protocol.BreakerEvent.{GameContainerAllState, GameContainerState, GameEvent, UserActionEvent}
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
                                    var canvasUnit: Int,
                                  ) extends GameContainer with EsRecover
  with BackDrawUtil
  with InfoDrawUtil {
  def debug(msg: String) = println("debug:" + msg)

  def info(msg: String): Unit = println("info:" + msg)

  private var gameContainerAllStateOpt: Option[GameContainerAllState] = None
  private var gameContainerStateOpt: Option[GameContainerState] = None
  protected var waitSyncData: Boolean = true

  private val uncheckedActionMap = mutable.HashMap[Byte, Long]() //serinum -> frame


  def updateClientSize(canvasS: Point, cUnit: Int) = {
    canvasUnit = cUnit
    canvasSize = canvasS
  }

  protected def handleGameContainerAllState(gameContainerAllState: GameContainerAllState) = {
    systemFrame = gameContainerAllState.f
    waitSyncData = false
  }

  def receiveGameContainerAllState(gameContainerAllState: GameContainerAllState) = {
    gameContainerAllStateOpt = Some(gameContainerAllState)
  }

  def receiveGameContainerState(gameContainerState: GameContainerState) = {
    if (gameContainerState.f > systemFrame) {

    } else if (gameContainerState.f == systemFrame) {

    } else {
      info(s"收到同步数据，但未同步，curSystemFrame=${systemFrame},sync game container state frame=${gameContainerState.f}")
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

  override protected def clearEventWhenUpdate(): Unit = {
    if (config.esRecoverSupport) {
      addEventHistory(systemFrame, gameEventMap.getOrElse(systemFrame, Nil), actionEventMap.getOrElse(systemFrame, Nil))
    }
    gameEventMap -= systemFrame
    actionEventMap -= systemFrame
    systemFrame += 1
  }

}

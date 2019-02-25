package com.neo.sk.breaker.shared.game.view

import com.neo.sk.breaker.shared.`object`.Ball
import com.neo.sk.breaker.shared.game.GameContainerClientImpl
import com.neo.sk.breaker.shared.model.Point

import com.neo.sk.breaker.shared.util.canvas.{MiddleContext, MiddleFrame,MiddleImage}
import scala.collection.mutable

/**
  * Created by hongruying on 2018/8/29
  */
trait BallDrawUtil { this:GameContainerClientImpl =>

  private val ball1 =drawFrame.createImage("/img/ballBlue.png")
  private val ball2 =drawFrame.createImage("/img/ballRed.png")
  private val ball3 =drawFrame.createImage("/img/ballg.png")

  private def generateCanvas(bullet:Ball) = {
    bullet.getBulletLevel(myBreakId) match {
      case 1 => ball1
      case 2 => ball2
      case _ => ball3
    }
  }

  private val canvasCacheMap = mutable.HashMap[Byte,Any]()

  def updateBallSize()={
    canvasCacheMap.clear()
  }

  protected def drawBalls(up:Boolean,offset:Point, offsetTime:Long) = {
    ballMap.values.foreach{ bullet =>
      val p = if(up){
        offset-bullet.getPosition4Animation(quadTree,offsetTime)
      }else{
        bullet.getPosition4Animation(quadTree,offsetTime) + offset
      }
      val cacheCanvas = canvasCacheMap.getOrElseUpdate(bullet.getBulletLevel(myBreakId), generateCanvas(bullet))
      val radius = bullet.getRadius
      ctx.drawImage(cacheCanvas, (p.x - bullet.getRadius) * canvasUnit, (p.y - bullet.getRadius) * canvasUnit,Some(radius*2 * canvasUnit, radius*2 * canvasUnit))
    }
  }
}

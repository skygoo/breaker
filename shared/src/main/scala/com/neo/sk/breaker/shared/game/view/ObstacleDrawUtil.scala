package com.neo.sk.breaker.shared.game.view

import com.neo.sk.breaker.shared.`object`.PropBox
import com.neo.sk.breaker.shared.game.GameContainerClientImpl
import com.neo.sk.breaker.shared.model.Constants.ObstacleType
import com.neo.sk.breaker.shared.model.{Constants, Point}

import scala.collection.mutable
import com.neo.sk.breaker.shared.util.canvas.MiddleContext

import scala.collection.mutable
/**
  * Created by sky
  * Date on 2019/2/2
  * Time at 下午9:03
  */
trait ObstacleDrawUtil{ this:GameContainerClientImpl =>

  private val obstacleCanvasCacheMap = mutable.HashMap[(Byte, Boolean), Any]()

  private val steelImg =drawFrame.createImage("/img/瓷砖2.png")
//  private val brickImg =drawFrame.createImage("/img/wall.png")
  private val addBallImg =drawFrame.createImage("/img/加号2.png")
  private val delBallImg =drawFrame.createImage("/img/减号2.png")


  protected def obstacleImgComplete: Boolean = steelImg.isComplete && addBallImg.isComplete && delBallImg.isComplete

  def updateObstacleSize()={
    obstacleCanvasCacheMap.clear()
  }

  private def generateObstacleCacheCanvas(width: Float, height: Float, color: String): Any = {
    val cacheCanvas = drawFrame.createCanvas((width * canvasUnit).toInt, (height * canvasUnit).toInt)
    val ctxCache = cacheCanvas.getCtx
    drawObstacle(Point(width / 2, height / 2), width, height, 1, color, ctxCache)
    cacheCanvas.change2Image()
  }

  private def drawObstacle(centerPosition:Point, width:Float, height:Float, bloodPercent:Float, color:String, context:MiddleContext = ctx):Unit = {
    context.setFill(color)
    context.setStrokeStyle(color)
    context.setLineWidth(2)
    context.beginPath()
    context.fillRec((centerPosition.x - width / 2) * canvasUnit, (centerPosition.y + height / 2 - height) * canvasUnit,
      width * canvasUnit, bloodPercent * height * canvasUnit)
    context.closePath()
    context.beginPath()
    context.rect((centerPosition.x - width / 2) * canvasUnit, (centerPosition.y - height / 2) * canvasUnit,
      width * canvasUnit, height * canvasUnit
    )
    context.stroke()
    context.closePath()
    context.setLineWidth(1)
  }

  protected def getPropImg(p:Byte)={
    p match {
      case Constants.PropType.addBallProp=>addBallImg
      case Constants.PropType.decBallProp=>delBallImg
      case _ => delBallImg
    }
  }

  protected def drawObstacles(up:Boolean,offset:Point) = {
    obstacleMap.values.foreach{ obstacle =>
      val color="rgba(139, 105, 105, 0.5)"
      val p =if(up){
        offset -obstacle.getPosition - Point(obstacle.getWidth / 2, obstacle.getHeight / 2)
      }else{
        obstacle.getPosition + offset - Point(obstacle.getWidth / 2, obstacle.getHeight / 2)
      }
      val isAttacked =  obstacleAttackedAnimationMap.contains(obstacle.oId)
      if(isAttacked){
        if (obstacleAttackedAnimationMap(obstacle.oId) <= 0) obstacleAttackedAnimationMap.remove(obstacle.oId)
        else obstacleAttackedAnimationMap.put(obstacle.oId, obstacleAttackedAnimationMap(obstacle.oId) - 1)
        ctx.setGlobalAlpha(0.5)
      }
      if(obstacle.obstacleType == ObstacleType.airDropBox){
        obstacle.propType.foreach(o =>ctx.drawImage(getPropImg(o), p.x * canvasUnit, p.y * canvasUnit,
          Some(obstacle.getWidth * canvasUnit, obstacle.getHeight * canvasUnit)))
      }else{
        val cache = obstacleCanvasCacheMap.getOrElseUpdate((obstacle.obstacleType, false), generateObstacleCacheCanvas(obstacle.getWidth, obstacle.getHeight, color))
        ctx.drawImage(cache, p.x * canvasUnit, p.y * canvasUnit)
//        ctx.drawImage(wallImg, p.x * canvasUnit, p.y * canvasUnit,
//          Some(obstacle.getWidth * canvasUnit, obstacle.getHeight * canvasUnit))
      }
      if(isAttacked){
        ctx.setGlobalAlpha(1)
      }
      ctx.setFont("Helvetica", "normal",2 * canvasUnit)
      ctx.setFill("rgb(0,0,0)")
      ctx.setTextAlign("center")
      ctx.fillText(obstacle.getCurBlood().toString,(p.x+obstacle.getWidth/2) * canvasUnit, (p.y+obstacle.getHeight/2) * canvasUnit)
      ctx.beginPath()
//      ctx.setFont("Helvetica", "normal",2 * canvasUnit)
//      ctx.setFill("rgb(0,0,0)")
//      ctx.setTextAlign("left")
//      ctx.fillText(obstacle.oId.toString,(p.x+obstacle.getWidth/2) * canvasUnit, (p.y+obstacle.getHeight/2) * canvasUnit)
//      ctx.beginPath()
    }
  }



  private def generateEnvironmentCacheCanvas(obstacleType:Byte, obstacleWidth:Float, obstacleHeight:Float,isAttacked:Boolean):Any = {
    val canvasCache = drawFrame.createCanvas(math.ceil(obstacleWidth * canvasUnit).toInt, math.ceil(obstacleHeight * canvasUnit).toInt)
    val ctxCache = canvasCache.getCtx
    if (!isAttacked){
      ctxCache.drawImage(steelImg, 0, 0,
        Some(obstacleWidth * canvasUnit,obstacleHeight * canvasUnit))
    } else{
      ctxCache.setGlobalAlpha(0.5)
      ctxCache.drawImage(steelImg, 0, 0,
        Some(obstacleWidth * canvasUnit,obstacleHeight * canvasUnit))
      ctxCache.setGlobalAlpha(1)
    }
    canvasCache.change2Image()
  }

  protected def drawEnvironment(up:Boolean,offset:Point) = {
    environmentMap.values.foreach { obstacle =>
      val p = if(up){
        offset-obstacle.getPosition - Point(obstacle.getWidth, obstacle.getHeight) / 2
      }else{
        obstacle.getPosition - Point(obstacle.getWidth, obstacle.getHeight) / 2 + offset
      }
      if (obstacleImgComplete) {
        val isAttacked =  obstacleAttackedAnimationMap.contains(obstacle.oId)
        val cacheCanvas = obstacleCanvasCacheMap.getOrElseUpdate((obstacle.obstacleType, isAttacked),
          generateEnvironmentCacheCanvas(obstacle.obstacleType, obstacle.getWidth, obstacle.getHeight, isAttacked))
        ctx.drawImage(cacheCanvas, p.x * canvasUnit, p.y * canvasUnit)
      } else {
        ctx.beginPath()
        ctx.drawImage(steelImg, p.x * canvasUnit, p.y * canvasUnit,
          Some(obstacle.getWidth * canvasUnit, obstacle.getHeight * canvasUnit))
        ctx.fill()
        ctx.stroke()
        ctx.closePath()
      }
      if (obstacleAttackedAnimationMap.contains(obstacle.oId)) {
        if (obstacleAttackedAnimationMap(obstacle.oId) <= 0) obstacleAttackedAnimationMap.remove(obstacle.oId)
        else obstacleAttackedAnimationMap.put(obstacle.oId, obstacleAttackedAnimationMap(obstacle.oId) - 1)
      }
    }

  }
}

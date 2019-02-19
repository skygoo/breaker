package com.neo.sk.breaker.shared.game.view

import com.neo.sk.breaker.shared.game.GameContainerClientImpl
import com.neo.sk.breaker.shared.model.Point
import com.neo.sk.breaker.shared.util.canvas.{MiddleCanvas, MiddleContext, MiddleFrame, MiddleImage}

import scala.collection.mutable

/**
  * Created by sky
  * Date on 2019/2/2
  * Time at 下午9:03
  */
trait BackDrawUtil {this:GameContainerClientImpl=>
  private val cacheCanvasMap = mutable.HashMap.empty[String, Any]


  def updateBackSize()={
    cacheCanvasMap.clear()
 }


  private def generateBackgroundCanvas() = {
    val cacheCanvas = drawFrame.createCanvas(((boundary.x + canvasSize.x) * canvasUnit).toInt,((boundary.y + canvasSize.y) * canvasUnit).toInt)
    val cacheCanvasCtx=cacheCanvas.getCtx
    clearScreen("#BEBEBE", 1, boundary.x + canvasSize.x, boundary.y + canvasSize.y, cacheCanvasCtx)
    clearScreen("#E8E8E8",1, boundary.x, boundary.y, cacheCanvas.getCtx, canvasSize / 2)

    cacheCanvasCtx.setLineWidth(1)
    cacheCanvasCtx.setStrokeStyle("rgba(0,0,0,0.5)")
    for(i <- 0  to((boundary.x + canvasSize.x).toInt,2)){
      drawLine(Point(i,0), Point(i, boundary.y + canvasSize.y), cacheCanvasCtx)
    }

    for(i <- 0  to((boundary.y + canvasSize.y).toInt,2)){
      drawLine(Point(0 ,i), Point(boundary.x + canvasSize.x, i), cacheCanvasCtx)
    }
    cacheCanvas.change2Image()
  }


  private def clearScreen(color:String, alpha:Double, width:Float = canvasSize.x, height:Float = canvasSize.y, middleCanvas:MiddleContext, start:Point = Point(0,0)):Unit = {
    middleCanvas.setFill(color)
    middleCanvas.setGlobalAlpha(alpha)
    middleCanvas.fillRec(start.x * canvasUnit, start.y * canvasUnit,  width * this.canvasUnit, height * this.canvasUnit)
    middleCanvas.setGlobalAlpha(1)
  }

  protected def drawLine(start:Point,end:Point, middleCanvas:MiddleContext):Unit = {
    middleCanvas.beginPath
    middleCanvas.moveTo(start.x * canvasUnit, start.y * canvasUnit)
    middleCanvas.lineTo(end.x * canvasUnit, end.y * canvasUnit)
    middleCanvas.stroke()
    middleCanvas.closePath()
  }


  /*protected def drawBackground(offset: Point) = {
    clearScreen("#FCFCFC", 1, canvasBoundary.x, canvasBoundary.y, ctx)
    val cacheCanvas = cacheCanvasMap.getOrElseUpdate("background", generateBackgroundCanvas())
    ctx.drawImage(cacheCanvas, (-offset.x + canvasBoundary.x / 2) * canvasUnit, (-offset.y + canvasBoundary.y / 2) * canvasUnit,
      Some(canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit))
  }*/

  protected def drawBackground() = {
    clearScreen("#E8E8E8",1, canvasSize.x, canvasSize.y, ctx)
  }
}

package com.neo.sk.breaker.shared.game.view

import com.neo.sk.breaker.shared.`object`.Breaker
import com.neo.sk.breaker.shared.game.GameContainerClientImpl
import com.neo.sk.breaker.shared.model.Point

import scala.collection.mutable
import com.neo.sk.breaker.shared.model.Constants.{BreakColor,ExpressionMap}
/**
  * Created by sky
  * Date on 2019/2/18
  * Time at 下午5:24
  */
trait BreakerDrawUtil {this:GameContainerClientImpl =>
  private val expressionInfoCacheMap = mutable.HashMap[Byte, Any]()

  private val blueTank =drawFrame.createImage("/img/blueTank.png")
  private val redTank =drawFrame.createImage("/img/redTank.png")
  private val talkImg =drawFrame.createImage("/img/对话框.png")


  def updateBreakSize()={
//    expressionInfoCacheMap.clear()
  }

  private def generateExpCacheCanvas(et:Byte):Any = {
    drawFrame.createImage("/img/exp/"+ExpressionMap.list.find(_._1==et).getOrElse(0,"")._2+".png")
  }



  protected def drawBreaker(up:Boolean,offset:Point, offsetTime:Long) = {
    breakMap.values.foreach { breaker =>
      val p =if(up){
        offset-breaker.getPosition4Animation(offsetTime)
      }else{
        breaker.getPosition4Animation(offsetTime) + offset
      }
      val gunPositionList = breaker.getGunPositions4Animation().map(t => (if(up) p-t else t+p) * canvasUnit)
      ctx.beginPath()
      ctx.moveTo(gunPositionList.last.x, gunPositionList.last.y)
      gunPositionList.foreach(t => ctx.lineTo(t.x, t.y))
      ctx.setFill("#7A7A7A")
      ctx.setStrokeStyle("#636363")
      ctx.fill()
      ctx.setLineWidth(0.4 * canvasUnit)
      ctx.stroke()
      ctx.closePath()

      ctx.beginPath()
      ctx.setLineWidth( 0.4 * canvasUnit)
      if(breaker.getBulletSize()>0) ctx.setStrokeStyle("#4EEE94") else ctx.setStrokeStyle("#636363")
      val centerX = p.x * canvasUnit
      val centerY = p.y * canvasUnit
      val radius =  (config.breakWidth / 2) * canvasUnit
      val startAngle = 0
      val lengthAngle = 360
      ctx.arc(centerX.toFloat, centerY.toFloat, radius, startAngle.toFloat, lengthAngle.toFloat)
      ctx.setFill(if(breaker.getBulletSize()>0) "#4EEE94" else "#636363")
      ctx.fill()
      ctx.stroke()
      ctx.closePath()
      ctx.setGlobalAlpha(1)
     val breakImg = if(breaker.up) blueTank else redTank
      ctx.drawImage(breakImg, (p.x-breaker.getWidth / 2) * canvasUnit, (p.y-breaker.getHeight / 2) * canvasUnit,
        Some(breaker.getWidth * canvasUnit, breaker.getHeight * canvasUnit))

      breaker.getExpression.foreach{e=>
        if(e._1>systemFrame-20){
          val pos=Point((p.x+breaker.getWidth / 2) * canvasUnit, (p.y-breaker.getHeight) * canvasUnit)
          ctx.drawImage(talkImg, pos.x,pos.y,
            Some(breaker.getWidth*5 * canvasUnit, breaker.getHeight*2 * canvasUnit))

          e._2 match {
            case ExpressionMap.e0=>
              ctx.setFill("#006699")
              ctx.setTextAlign("center")
              ctx.setFont("楷体", "normal", 2 * canvasUnit)
              ctx.setLineWidth(2)
              ctx.fillText(s"${e._3.getOrElse("")}", pos.x, pos.y, 20 * canvasUnit)
            case _=>
              ctx.drawImage(expressionInfoCacheMap.getOrElseUpdate(e._2,generateExpCacheCanvas(e._2)), pos.x+2*canvasUnit, (p.y-breaker.getHeight / 2) * canvasUnit,
                Some(breaker.getWidth * canvasUnit, breaker.getHeight * canvasUnit))
          }
        }
      }

      ctx.beginPath()
      val namePosition = (p + Point(0, 5)) * canvasUnit
      ctx.setFill("#006699")
      ctx.setTextAlign("center")
      ctx.setFont("楷体", "normal", 2 * canvasUnit)
      ctx.setLineWidth(2)
      ctx.fillText(s"${breaker.name}", namePosition.x, namePosition.y, 20 * canvasUnit)
      ctx.closePath()
    }
  }
}

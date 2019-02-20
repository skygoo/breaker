package com.neo.sk.breaker.shared.game.view

import com.neo.sk.breaker.shared.`object`.Breaker
import com.neo.sk.breaker.shared.game.GameContainerClientImpl
import com.neo.sk.breaker.shared.model.Point

import scala.collection.mutable
import com.neo.sk.breaker.shared.model.Constants.BreakColor
/**
  * Created by sky
  * Date on 2019/2/18
  * Time at 下午5:24
  */
trait BreakerDrawUtil {this:GameContainerClientImpl =>
  private val myTankInfoCacheMap = mutable.HashMap[(Byte,Byte,Byte), Any]()

  private val blueTank =drawFrame.createImage("/img/blueTank.png")
  private val redTank =drawFrame.createImage("/img/redTank.png")

  def updateBreakSize()={
    myTankInfoCacheMap.clear()
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
     val tankColor = if(breaker.up) blueTank else redTank
      ctx.drawImage(tankColor, (p.x-breaker.getWidth / 2) * canvasUnit, (p.y-breaker.getHeight / 2) * canvasUnit,
        Some(breaker.getWidth * canvasUnit, breaker.getHeight * canvasUnit))


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

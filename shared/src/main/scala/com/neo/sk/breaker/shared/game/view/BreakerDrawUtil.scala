package com.neo.sk.breaker.shared.game.view

import com.neo.sk.breaker.shared.`object`.Breaker
import com.neo.sk.breaker.shared.game.GameContainerClientImpl
import com.neo.sk.breaker.shared.model.Point

import scala.collection.mutable
import com.neo.sk.breaker.shared.model.Constants.{BreakColor, ExpressionMap, GameAnimation, RoomType}

/**
  * Created by sky
  * Date on 2019/2/18
  * Time at 下午5:24
  */
trait BreakerDrawUtil {
  this: GameContainerClientImpl =>
  private val expressionInfoCacheMap = mutable.HashMap[Byte, Any]()

  private val blueTank = drawFrame.createImage("/img/bluep.png")
  private val redTank = drawFrame.createImage("/img/redp.png")
  private val talkImg = drawFrame.createImage("/img/对话框.png")


  def updateBreakSize() = {
    //    expressionInfoCacheMap.clear()
  }

  private def generateExpCacheCanvas(et: Byte): Any = {
    drawFrame.createImage("/img/exp/" + ExpressionMap.list.find(_._1 == et).getOrElse(0, "")._2 + ".png")
  }


  protected def drawBreaker(up: Boolean, offset: Point, offsetTime: Long) = {
    breakMap.values.foreach { breaker =>
      val p4a = breaker.getPosition4Animation(boundary, quadTree, offsetTime)
      val p = if (up) {
        offset - p4a
      } else {
        p4a + offset
      }

      val gunPositionList = breaker.getGunPositions4Animation().map(t => (if (up) p - t else t + p) * canvasUnit)
      ctx.beginPath()
      ctx.moveTo(gunPositionList.last.x, gunPositionList.last.y)
      gunPositionList.foreach(t => ctx.lineTo(t.x, t.y))
      val color = if (breaker.breakId == myBreakId) BreakColor.blue else BreakColor.red
      ctx.setFill(color)
      ctx.setStrokeStyle("#636363")
      ctx.fill()
      ctx.setLineWidth(0.4 * canvasUnit)
      ctx.stroke()
      ctx.closePath()

      val breakImg = if (breaker.breakId == myBreakId) blueTank else redTank
      ctx.drawImage(breakImg, (p.x - breaker.getWidth / 2) * canvasUnit, (p.y - breaker.getHeight / 2) * canvasUnit,
        Some(breaker.getWidth * canvasUnit, breaker.getHeight * canvasUnit))

      breaker.getExpression.foreach { e =>
        if (e._1 > systemFrame - GameAnimation.talkAnimationFrame) {
          val pos = Point((p.x + breaker.getWidth / 2) * canvasUnit, (p.y - 5) * canvasUnit)
          ctx.drawImage(talkImg, pos.x, pos.y,
            Some(30 * canvasUnit, 10 * canvasUnit))

          e._2 match {
            case ExpressionMap.e0 =>
              ctx.beginPath()
              ctx.setFill("#006699")
              ctx.setTextAlign("left")
              ctx.setFont("楷体", "normal", 3 * canvasUnit)
              ctx.setLineWidth(2)
              ctx.fillText(s"${e._3.getOrElse("")}", pos.x + (breaker.getWidth / 2) * canvasUnit, (p.y - 1) * canvasUnit, breaker.getWidth * 5 * canvasUnit)
              ctx.closePath()
            case _ =>
              ctx.drawImage(expressionInfoCacheMap.getOrElseUpdate(e._2, generateExpCacheCanvas(e._2)), pos.x + 2 * canvasUnit, (p.y - 3) * canvasUnit,
                Some(6 * canvasUnit, 6 * canvasUnit))
          }
        }
      }

      ctx.beginPath()
      val namePosition = (p + Point(0, 2)) * canvasUnit
      ctx.setFill("#006699")
      ctx.setTextAlign("center")
      ctx.setFont("楷体", "normal", 2 * canvasUnit)
      ctx.setLineWidth(2)
      ctx.fillText(s"${breaker.name}", namePosition.x, namePosition.y, 20 * canvasUnit)
      ctx.closePath()
    }
  }

  val length=10
  def changeFrame2Time(f:Long)={
    val frame=f/10
    val min=frame/60
    val sec=frame-(min*60)
    s"$min 分 $sec 秒"
  }
  protected def drawMyTankInfo(break: Breaker) = {
    ctx.beginPath()
    ctx.setStrokeStyle("rgb(0,0,0)")
    ctx.setTextAlign("left")
    ctx.setFont("隶书", "bold", 1.8 * canvasUnit)
    ctx.setLineWidth(1)
    ctx.fillText(s"发射状态", 4.5 * canvasUnit, 5.5 * canvasUnit, 30 * canvasUnit)
    ctx.fillText(s"撞击得分", 4.5 * canvasUnit, 10 * canvasUnit, 30 * canvasUnit)
    ctx.fillText(break.crashCount.toString, 15 * canvasUnit, 10 * canvasUnit, 30 * canvasUnit)
    ctx.fillText(s"游戏倒计时", 4.5 * canvasUnit, 14.5 * canvasUnit, 30 * canvasUnit)
    ctx.fillText(changeFrame2Time(config.gameMaxFrame-systemFrame), 15 * canvasUnit, 14.5 * canvasUnit, 30 * canvasUnit)
    ctx.setLineWidth(3)
    ctx.moveTo(15 * canvasUnit, 5.5 * canvasUnit)
    ctx.lineTo((15+length) * canvasUnit, 5.5 * canvasUnit)
    ctx.lineTo((15+length) * canvasUnit, 7.5 *canvasUnit)
    ctx.lineTo(15 * canvasUnit, 7.5 * canvasUnit)
    ctx.lineTo(15 * canvasUnit, 5.5 * canvasUnit)
    ctx.stroke()
    ctx.closePath()
    ctx.beginPath()
    ctx.setLineWidth(1)
    ctx.moveTo(15 * canvasUnit, 5.5 * canvasUnit)
    ctx.lineTo((15+length*break.getBulletPercent()) * canvasUnit, 5.5 * canvasUnit)
    ctx.lineTo((15+length*break.getBulletPercent()) * canvasUnit, 7.5 *canvasUnit)
    ctx.lineTo(15 * canvasUnit, 7.5 * canvasUnit)
    ctx.lineTo(15 * canvasUnit, 5.5 * canvasUnit)
    ctx.setFill("#006699")
    ctx.setStrokeStyle("#636363")
    ctx.fill()
    ctx.stroke()
    ctx.closePath()
  }


}

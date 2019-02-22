package com.neo.sk.breaker.shared.`object`

import com.neo.sk.breaker.shared.model
import com.neo.sk.breaker.shared.model.{Point, Segment}

/**
  * Created by hongruying on 2018/8/22
  * 矩形游戏物体元素
  */
trait RectangleObjectOfGame extends ObjectOfGame {


  protected val width: Float
  protected val height: Float
  protected val collisionOffset: Float //？

  val pos: Byte

  final def getWidth = width

  final def getHeight = height

  private[this] def collisionWidth = width - collisionOffset
  private[this] def collisionHeight = height - collisionOffset
  /**
    * 获取当前元素的包围盒
    *
    * @return rectangle
    */
  override def getObjectRect(): model.Rectangle = {
    model.Rectangle(position - model.Point(width / 2, height / 2), position + model.Point(width / 2, height / 2))
  }

  /**
    * 获取当前元素的外形
    *
    * @return shape
    */
  override def getObjectShape(): model.Shape = {
    getObjectRect()
  }

  /**
    * 判断元素是否和其他元素有碰撞
    *
    * @param o 其他物体
    * @return 如果碰撞，返回true；否则返回false
    */
  override def isIntersects(o: ObjectOfGame): (Boolean,Boolean) = {
    o match {
      case t: CircleObjectOfGame => isIntersects(t)
      case t: RectangleObjectOfGame => isIntersects(t)
    }
  }

  private def isIntersects(o: CircleObjectOfGame): (Boolean,Boolean) = {
    val topLeft = position - model.Point(collisionWidth / 2, collisionHeight / 2)
    val downRight = position + model.Point(collisionWidth / 2, collisionHeight / 2)
    val topRight = position + model.Point(collisionWidth / 2, -collisionHeight / 2)
    val downLeft = position + model.Point(-collisionWidth / 2, collisionHeight / 2)
    val lineSegment: Segment = pos match {
      case 0 => Segment()
      case 1 => new Segment(topLeft, downRight)
      case 2 => new Segment(topRight, downLeft)
      case 3 => new Segment(topLeft, downRight)
      case _ => new Segment(topRight, downLeft)
    }
    pos match {
      /** 矩形 */
      case 0 =>
        (if (o.getPosition > topLeft && o.getPosition < downRight) {
          true
        } else {
          val relativeCircleCenter: Point = o.getPosition - position
          val dx = math.min(relativeCircleCenter.x, width / 2)
          val dx1 = math.max(dx, -width / 2)
          val dy = math.min(relativeCircleCenter.y, height / 2)
          val dy1 = math.max(dy, -height / 2)
          Point(dx1, dy1).distance(relativeCircleCenter) < o.radius
        },false)

      /** 右上三角 */
      case 1 =>
        if (lineSegment.distanceFromPoint(o.getPosition) < o.radius*0.8) {
          (true,true)
        } else if (lineSegment.directionFromPoint(o.getPosition)) {
          val relativeCircleCenter: Point = o.getPosition - position
          val dx = math.min(relativeCircleCenter.x, width / 2)
          val dx1 = math.max(dx, -width / 2)
          val dy = math.min(relativeCircleCenter.y, height / 2)
          val dy1 = math.max(dy, -height / 2)
          (Point(dx1, dy1).distance(relativeCircleCenter) < o.radius,false)
        } else {
          (false,false)
        }


      /** 左上三角 */
      case 2 =>
        if (lineSegment.distanceFromPoint(o.getPosition) < o.radius*0.8) {
          (true,true)
        } else if (lineSegment.directionFromPoint(o.getPosition)) {
          val relativeCircleCenter: Point = o.getPosition - position
          val dx = math.min(relativeCircleCenter.x, width / 2)
          val dx1 = math.max(dx, -width / 2)
          val dy = math.min(relativeCircleCenter.y, height / 2)
          val dy1 = math.max(dy, -height / 2)
          (Point(dx1, dy1).distance(relativeCircleCenter) < o.radius,false)
        } else {
          (false,false)
        }

      /** 左下三角 */
      case 3 =>
        if (lineSegment.distanceFromPoint(o.getPosition) < o.radius*0.8) {
          (true,true)
        } else if (!lineSegment.directionFromPoint(o.getPosition)) {
          val relativeCircleCenter: Point = o.getPosition - position
          val dx = math.min(relativeCircleCenter.x, width / 2)
          val dx1 = math.max(dx, -width / 2)
          val dy = math.min(relativeCircleCenter.y, height / 2)
          val dy1 = math.max(dy, -height / 2)
          (Point(dx1, dy1).distance(relativeCircleCenter) < o.radius,false)
        } else {
          (false,false)
        }

      /** 右下三角 */
      case _ =>
        if (lineSegment.distanceFromPoint(o.getPosition) < o.radius*0.8) {
          (true,true)
        } else if (!lineSegment.directionFromPoint(o.getPosition)) {
          val relativeCircleCenter: Point = o.getPosition - position
          val dx = math.min(relativeCircleCenter.x, width / 2)
          val dx1 = math.max(dx, -width / 2)
          val dy = math.min(relativeCircleCenter.y, height / 2)
          val dy1 = math.max(dy, -height / 2)
          (Point(dx1, dy1).distance(relativeCircleCenter) < o.radius,false)
        } else {
          (false,false)
        }
    }
  }

  private def isIntersects(o: RectangleObjectOfGame): (Boolean,Boolean) = {
    (getObjectRect().intersects(o.getObjectRect()),false)
  }

}

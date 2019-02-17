package com.neo.sk.breaker.shared.`object`

import com.neo.sk.breaker.shared.model
import com.neo.sk.breaker.shared.model.Point

/**
  * Created by sky
  * Date on 2019/2/13
  * Time at 下午9:47
  * 三角形
  */
trait TriangleObjectOfGame extends RectangleObjectOfGame{

  /**
    * 判断元素是否和其他元素有碰撞
    * @param o 其他物体
    * @return  如果碰撞，返回true；否则返回false
    */
  override def isIntersects(o: ObjectOfGame): Boolean = {
    o match {
      case t:CircleObjectOfGame => isIntersects(t)
      case t:RectangleObjectOfGame => isIntersects(t)
    }
  }

  private def isIntersects(o: CircleObjectOfGame): Boolean = {
    val topLeft = position - model.Point(collisionWidth / 2, collisionHeight / 2)
    val downRight = position + model.Point(collisionWidth / 2, collisionHeight / 2)
    if(o.getPosition > topLeft && o.getPosition < downRight){
      true
    }else{
      val relativeCircleCenter:Point = o.getPosition - position
      val dx = math.min(relativeCircleCenter.x, collisionWidth / 2)
      val dx1 = math.max(dx, - collisionHeight / 2)
      val dy = math.min(relativeCircleCenter.y, collisionHeight / 2)
      val dy1 = math.max(dy, - collisionHeight / 2)
      Point(dx1,dy1).distance(relativeCircleCenter) < o.radius
    }
  }

  private def isIntersects(o: RectangleObjectOfGame): Boolean = {
    getObjectRect().intersects(o.getObjectRect())
  }
}

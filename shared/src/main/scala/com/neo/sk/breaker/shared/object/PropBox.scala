package com.neo.sk.breaker.shared.`object`

import com.neo.sk.breaker.shared.game.config.BreakGameConfig
import com.neo.sk.breaker.shared.model.Constants.ObstacleType
import com.neo.sk.breaker.shared.model.Point

/**
  * Created by hongruying on 2018/8/22
  * 道具箱
  */
case class PropBox(
                       config:BreakGameConfig,
                       override val oId: Int,
                       override protected var position: Point,
                       protected var curBlood :Int, //物体血量
                       propType:Byte
                     ) extends Obstacle with ObstacleBall {

  def this(config: BreakGameConfig,obstacleState: ObstacleState){
    this(config,obstacleState.oId,obstacleState.p,obstacleState.b.getOrElse(config.propMaxBlood),obstacleState.pt.getOrElse(0))
  }

  override val obstacleType = ObstacleType.airDropBox
  override protected val height: Float = config.obstacleWidth
  override protected val width: Float = config.obstacleWidth
  override protected val collisionOffset: Float = config.obstacleWO



  val maxBlood : Int = config.propMaxBlood

  def getObstacleState():ObstacleState = ObstacleState(oId,obstacleType,Some(propType),Some(curBlood),position)


  override def attackDamage(d: Int): Unit = {
    curBlood -= d
  }

  override def isLived(): Boolean = {
    if(curBlood > 0) true
    else false
  }

  override def bloodPercent():Float = curBlood.toFloat / maxBlood

}

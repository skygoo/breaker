package com.neo.sk.breaker.shared.`object`

import com.neo.sk.breaker.shared.game.config.BreakGameConfig
import com.neo.sk.breaker.shared.model.Constants.ObstacleType
import com.neo.sk.breaker.shared.model.Point

/**
  * Created by hongruying on 2018/8/22
  * 砖头元素
  */
case class Brick(
                  config:BreakGameConfig,
                  override val oId: Int,
                  override val pos: Byte = 0,
                  override protected var position: Point,
                  protected var curBlood :Int //物体血量
                ) extends Obstacle with ObstacleBall {

  def this(config: BreakGameConfig,obstacleState: ObstacleState){
    this(config,obstacleState.oId,obstacleState.pos,obstacleState.p,obstacleState.b.getOrElse(config.brickMaxBlood))
  }

  override val obstacleType = ObstacleType.brick
  override protected val height: Float = 5
  override protected val width: Float = 5
  override protected val collisionOffset: Float = config.obstacleWO


  def getObstacleState():ObstacleState = ObstacleState(oId,obstacleType,pos,None,Some(curBlood),position)

  override def attackDamage(d: Int): Unit = {
    curBlood -= d
  }

  override def isLived(): Boolean = {
    if(curBlood > 0) true
    else false
  }

  override def getCurBlood():Int = curBlood

}

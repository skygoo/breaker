package com.neo.sk.breaker.shared.`object`

import com.neo.sk.breaker.shared.game.config.BreakGameConfig
import com.neo.sk.breaker.shared.model.Constants.ObstacleType
import com.neo.sk.breaker.shared.model.Point

/**
  * Created by sky
  * Date on 2019/2/15
  * Time at 下午3:21
  * 包围墙，不可打击
  */
case class Wall (
             config:BreakGameConfig,
             override val oId: Int,
             override protected var position: Point,
           ) extends Obstacle with ObstacleBreak {

  def this(config: BreakGameConfig,obstacleState: ObstacleState){
    this(config,obstacleState.oId,obstacleState.p)
  }

  override val obstacleType = ObstacleType.wall
  override protected val height: Float = config.obstacleWidth
  override protected val width: Float = config.obstacleWidth
  override protected val collisionOffset: Float = config.obstacleWO

  override def getObstacleState():ObstacleState = ObstacleState(oId,obstacleType,None,None,position)

  override def attackDamage(d: Int): Unit = {}

  override def isLived(): Boolean = true

  override def bloodPercent():Float = 1

}
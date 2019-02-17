package com.neo.sk.breaker.shared.`object`

import com.neo.sk.breaker.shared.game.config.BreakGameConfig
import com.neo.sk.breaker.shared.model.Point

/**
  * Created by sky
  * Date on 2019/2/15
  * Time at 下午2:33
  * 游戏角色
  * 左右移动
  *
  */
case class BreakState(playerId: String, breakId: Int, name: String, position: Point)

case class Breaker(
                    config: BreakGameConfig,
                    playerId: String,
                    breakId: Int,
                    name: String,
                    override protected var position: Point
                  )extends RectangleObjectOfGame with ObstacleBreak {
  override protected val height: Float = config.obstacleWidth
  override protected val width: Float = config.obstacleWidth
  override protected val collisionOffset: Float = config.obstacleWO
}

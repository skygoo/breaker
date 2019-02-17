package com.neo.sk.breaker.core.game


import com.neo.sk.breaker.shared.model.Point
import com.typesafe.config.Config
import akka.util.Helpers
import com.neo.sk.breaker.shared.game.config
import com.neo.sk.breaker.shared.model.Constants.PropAnimation

import scala.concurrent.duration._
import com.neo.sk.breaker.shared.game.config._

/**
  * Created by sky
  * Date on 2019/2/13
  * Time at 下午5:03
  */
case class BreakGameConfigServerImpl(config: Config){
  import collection.JavaConverters._
  import Helpers.Requiring
  import Helpers.ConfigOps

  private[this] val gridBoundaryWidth = config.getInt("tankGame.gridBoundary.width")
    .requiring(_ > 100,"minimum supported grid boundary width is 100")
  private[this] val gridBoundaryHeight = config.getInt("tankGame.gridBoundary.height")
    .requiring(_ > 50,"minimum supported grid boundary height is 50")
  private[this] val gridBoundary = GridBoundary(gridBoundaryWidth,gridBoundaryHeight)

  private[this] val gameFameDuration = config.getLong("tankGame.frameDuration")
    .requiring(t => t >= 1l,"minimum game frame duration is 1 ms")

  private[this] val ballRadius = config.getDoubleList("tankGame.ball.ballRadius")
    .requiring(_.size() >= 3,"ball radius size has 3 type").asScala.toList.map(_.toFloat)
  private[this] val ballDamage = config.getIntList("tankGame.ball.ballDamage")
    .requiring(_.size() >= 3,"ball damage size has 3 type").asScala.toList.map(_.toInt)
  private[this] val maxFly = config.getInt("tankGame.ball.maxFlyFrame").toByte
    .requiring(_ > 0,"minimum ball max fly frame is 1")
  private[this] val ballSpeedData = config.getInt("tankGame.ball.ballSpeed")
    .requiring(_ > 0,"minimum ball speed is 1")
  private val ballParameters = BallParameters(ballRadius.zip(ballDamage),maxFly,ballSpeedData)

  private[this] val obstacleWidthData = config.getDouble("tankGame.obstacle.width")
    .requiring(_ > 0,"minimum supported obstacle width is 1").toFloat
  private[this] val collisionWOffset = config.getDouble("tankGame.obstacle.collisionWidthOffset")
    .requiring(_ > 0,"minimum supported obstacle width is 1").toFloat


  private[this] val airDropBloodData = config.getInt("tankGame.obstacle.airDrop.blood").toByte
    .requiring(_ > 0,"minimum supported air drop blood is 1")
  private[this] val airDropNumData = config.getInt("tankGame.obstacle.airDrop.num")
    .requiring(_ >= 0,"minimum supported air drop num is 0")

  private[this] val brickBloodData = config.getInt("tankGame.obstacle.brick.blood").toByte
    .requiring(_ > 0,"minimum supported brick blood is 1")
  private[this] val brickNumData = config.getInt("tankGame.obstacle.brick.num")
    .requiring(_ >= 0,"minimum supported brick num is 0")

  private val obstacleParameters = ObstacleParameters(obstacleWidthData,collisionWOffset,
    propParameters = PropParameters(airDropBloodData,airDropNumData),
    brickParameters = BrickParameters(brickBloodData,brickNumData)
  )

  val gameConfig = BreakGameConfigImpl(gridBoundary,gameFameDuration,ballParameters,obstacleParameters)

}

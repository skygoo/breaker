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

  private[this] val gridBoundaryWidth = config.getInt("breakerGame.gridBoundary.width")
    .requiring(_ > 10,"minimum supported grid boundary width is 100")
  private[this] val gridBoundaryHeight = config.getInt("breakerGame.gridBoundary.height")
    .requiring(_ > 50,"minimum supported grid boundary height is 50")
  private[this] val gridBoundary = GridBoundary(gridBoundaryWidth,gridBoundaryHeight)

  private[this] val gameFameDuration = config.getLong("breakerGame.frameDuration")
    .requiring(t => t >= 1l,"minimum game frame duration is 1 ms")

  private[this] val ballRadius = config.getDoubleList("breakerGame.ball.ballRadius")
    .requiring(_.size() >= 2,"ball radius size has 2 type").asScala.toList.map(_.toFloat)
  private[this] val ballDamage = config.getIntList("breakerGame.ball.ballDamage")
    .requiring(_.size() >= 2,"ball damage size has 2 type").asScala.toList.map(_.toInt)
  private[this] val maxFly = config.getInt("breakerGame.ball.maxFly").toByte
    .requiring(_ > 0,"minimum ball max fly is 1")
  private[this] val ballSpeedData = config.getInt("breakerGame.ball.ballSpeed")
    .requiring(_ > 0,"minimum ball speed is 1")
  private val ballParameters = BallParameters(ballRadius.zip(ballDamage),maxFly,ballSpeedData)

  private[this] val obstacleWidthData = config.getDouble("breakerGame.obstacle.width")
    .requiring(_ > 0,"minimum supported obstacle width is 1").toFloat
  private[this] val collisionWOffset = config.getDouble("breakerGame.obstacle.collisionWidthOffset")
    .requiring(_ > 0,"minimum supported obstacle width is 1").toFloat


  private[this] val propBloodData = config.getInt("breakerGame.obstacle.airDrop.blood").toByte
    .requiring(_ > 0,"minimum supported air drop blood is 1")
  private[this] val propNumData = config.getInt("breakerGame.obstacle.airDrop.num")
    .requiring(_ >= 0,"minimum supported air drop num is 0")

  private[this] val brickBloodData = config.getInt("breakerGame.obstacle.brick.blood").toByte
    .requiring(_ > 0,"minimum supported brick blood is 1")
  private[this] val brickNumData = config.getInt("breakerGame.obstacle.brick.num")
    .requiring(_ >= 0,"minimum supported brick num is 0")

  private val obstacleParameters = ObstacleParameters(obstacleWidthData,collisionWOffset,
    propParameters = PropParameters(propBloodData,propNumData),
    brickParameters = BrickParameters(brickBloodData,brickNumData)
  )

  private[this] val breakRadiusData = config.getDouble("breakerGame.break.radius")
    .requiring(_ > 0,"minimum supported break radius is 1").toFloat
  private[this] val breakGunWidthData = config.getInt("breakerGame.break.gunWidth")
    .requiring(_ > 0,"minimum supported break gun width is 1")
  private[this] val breakGunHeightData = config.getInt("breakerGame.break.gunHeight")
    .requiring(_ > 0,"minimum supported break gun height is 1")
  private[this] val breakFillBulletFrame = config.getInt("breakerGame.break.fillBulletFrame")
    .requiring(_ > 0,"minimum supported break fillBulletFrame is 1")

  private val breakParameters = BreakerParameters(breakRadiusData,breakGunWidthData,breakGunHeightData,breakFillBulletFrame)

  val gameConfig = BreakGameConfigImpl(gridBoundary,gameFameDuration,ballParameters,obstacleParameters,breakParameters)

}

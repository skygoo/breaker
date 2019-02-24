package com.neo.sk.breaker.shared.game.config

import com.neo.sk.breaker.shared.model.Point

/**
  * Created by sky
  * Date on 2019/2/13
  * Time at 下午4:16
  */
final case class GridBoundary(width: Int, height: Int, column:Int) {
  def getBoundary: Point = Point(width, height)
}

final case class GridLittleMap(width: Int, height: Int) {
  def getBoundary: Point = Point(width, height)
}

final case class BreakerMoveSpeed(
                                   speeds: List[Int]
                                 ) {
  def getBreakerSpeedByType(t: Byte) = Point(speeds(t - 1), 0)
}

final case class BreakerParameters(
                                    //                                    breakSpeed: BreakerMoveSpeed,
                                    breakWidth1: Float,
                                    breakHeight1: Float,
                                    breakWidth2: Float,
                                    breakHeight2: Float,
                                    breakGunWidth: Float,
                                    breakGunHeight: Float,
                                    fillBulletFrame: Int,
                                    bulletLimit:Int,
                                    breakSpeed:Int
                                  )

final case class PropParameters(
                                 blood: Byte,
                                 num: Int
                               )

final case class BrickParameters(
                                  blood: Byte,
                                  num: Int
                                )

final case class ObstacleParameters(
                                     width: Float,
                                     collisionWidthOffset: Float,
                                     propParameters: PropParameters,
                                     brickParameters: BrickParameters
                                   )

final case class BallParameters(
                                 ballLevelParameters: List[(Float, Int)], //size,damage length 3
                                 maxFly: Byte,
                                 ballSpeed: Int,
                               ) {
  require(ballLevelParameters.size >= 2, println(s"ball level parameter failed"))

  def getBallRadius(l: Byte) = {
    ballLevelParameters(l - 1)._1
  }

  def getBallDamage(l: Byte) = {
    ballLevelParameters(l - 1)._2
  }

  def getBallRadiusByDamage(d: Int): Float = {
    ballLevelParameters.find(_._2 == d).map(_._1).getOrElse(ballLevelParameters.head._1)
  }

  def getBallLevelByDamage(d: Int): Byte = {
    (ballLevelParameters.zipWithIndex.find(_._1._2 == d).map(_._2).getOrElse(0) + 1).toByte
  }
}

case class BreakGameConfigImpl(
                                gridBoundary: GridBoundary,
                                frameDuration: Long,
                                gameMaxFrame: Long,
                                ballParameters: BallParameters,
                                obstacleParameters: ObstacleParameters,
                                breakerParameters: BreakerParameters
                              ) extends BreakGameConfig {
  def getBreakGameConfigImpl(): BreakGameConfigImpl = this

  val ballSpeed: Point = Point(ballParameters.ballSpeed, 0)

  val ballMaxFly: Byte = ballParameters.maxFly

  override def getBallRadius(l: Byte): Float = ballParameters.getBallRadius(l)

  def getBallDamage(l: Byte): Int = ballParameters.getBallDamage(l)

  def getBallLevel(damage: Int): Byte = ballParameters.getBallLevelByDamage(damage)

  def getBallMaxLevel(): Byte = ballParameters.ballLevelParameters.size.toByte

  def getBallRadiusByDamage(d: Int): Float = ballParameters.getBallRadiusByDamage(d)

  val boundary: Point = gridBoundary.getBoundary

  val gridColumn:Int = gridBoundary.column

  val obstacleWidth: Float = obstacleParameters.width

  val obstacleWO: Float = obstacleParameters.collisionWidthOffset

  val propMaxBlood: Byte = obstacleParameters.propParameters.blood

  val propNum: Int = obstacleParameters.propParameters.num

  val brickMaxBlood: Byte = obstacleParameters.brickParameters.blood

  val brickNum: Int = obstacleParameters.brickParameters.num

  override val breakGunWidth: Float = breakerParameters.breakGunWidth

  override val breakWidth1: Float = breakerParameters.breakWidth1

  override val breakHeight1: Float = breakerParameters.breakHeight1

  override val breakWidth2: Float = breakerParameters.breakWidth2

  override val breakHeight2: Float = breakerParameters.breakHeight2

  override val breakGunHeight: Float = breakerParameters.breakGunHeight

  override val fillBallFrame: Int = breakerParameters.fillBulletFrame

  override val breakBallLimit: Int = breakerParameters.bulletLimit

  override val breakSpeed: Int = breakerParameters.breakSpeed
}

package com.neo.sk.breaker.shared.game.config

import com.neo.sk.breaker.shared.model.Point

/**
  * Created by sky
  * Date on 2019/2/13
  * Time at 下午4:15
  */
final case class GridBoundary(width:Int,height:Int){
  def getBoundary:Point = Point(width,height)
}

final case class GridLittleMap(width:Int,height:Int){
  def getBoundary:Point = Point(width,height)
}

final case class BreakerMoveSpeed(
                                speeds:List[Int]
                              ){
  def getBreakerSpeedByType(t:Byte) = Point(speeds(t-1),0)
}

final case class BreakerParameters(
                                    tankSpeed:BreakerMoveSpeed
                                 ){

}

final case class PropParameters(
                                 radius:Float,
                                 medicalBlood:Int,
                                 shotgunDuration:Int, //散弹持续时间
                                 disappearTime:Int
                               )

final case class AirDropParameters(
                                    blood:Int,
                                    num:Int
                                  )

final case class BrickParameters(
                                  blood:Int,
                                  num:Int
                                )

final case class ObstacleParameters(
                                     width:Float,
                                     collisionWidthOffset: Float,
                                     airDropParameters: AirDropParameters,
                                     brickParameters: BrickParameters
                                   )

final case class BulletParameters(
                                   bulletLevelParameters:List[(Float,Int)], //size,damage length 3
                                   maxFlyFrame:Int,
                                   bulletSpeed:Int,
                                 ){
  require(bulletLevelParameters.size >= 3,println(s"bullet level parameter failed"))

  def getBulletRadius(l:Byte) = {
    bulletLevelParameters(l-1)._1
  }

  def getBulletDamage(l:Byte) = {
    bulletLevelParameters(l-1)._2
  }

  def getBulletRadiusByDamage(d:Int):Float = {
    bulletLevelParameters.find(_._2 == d).map(_._1).getOrElse(bulletLevelParameters.head._1)
  }

  def getBulletLevelByDamage(d:Int):Byte = {
    (bulletLevelParameters.zipWithIndex.find(_._1._2 == d).map(_._2).getOrElse(0) + 1).toByte
  }
}

trait BreakGameConfig {
  val frameDuration:Long

  val maxFlyFrame:Int

  val bulletSpeed:Point

  val boundary:Point

  val obstacleWidth:Float

  val obstacleWO: Float

  val airDropBlood:Int

  val airDropNum:Int

  val brickBlood:Int

  val brickNum:Int
}

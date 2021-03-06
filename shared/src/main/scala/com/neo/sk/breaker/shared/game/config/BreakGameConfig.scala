package com.neo.sk.breaker.shared.game.config

import com.neo.sk.breaker.shared.model.Point

/**
  * Created by sky
  * Date on 2019/2/13
  * Time at 下午4:15
  */
trait BreakGameConfig {
  def getBreakGameConfigImpl(): BreakGameConfigImpl

  val esRecoverSupport: Boolean = true

  val totalWallRow = 15

  val brickRow = 7

  val totalColumn = 6

  val brickColumn = 4

  val frameDuration: Long

  val gameMaxFrame: Long

  val ballSpeed: Point

  val ballMaxFly: Byte

  val fillBallFrame:Int

  def getBallRadius(l: Byte): Float

  def getBallDamage(l: Byte): Int

  def getBallLevel(damage: Int): Byte

  def getBallMaxLevel(): Byte

  def getBallRadiusByDamage(d: Int): Float

  val boundary: Point

  val gridColumn:Int

  val obstacleWidth: Float

  val obstacleWO: Float

  val propMaxBlood: Byte

  val propNum: Int

  val brickMaxBlood: Byte

  val brickNum: Int

  val breakWidth1:Float

  val breakHeight1:Float

  val breakWidth2:Float

  val breakHeight2:Float

  val breakBallLimit:Int

  val breakSpeed:Int

  val breakGunWidth:Float

  val breakGunHeight:Float

}

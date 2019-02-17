package com.neo.sk.breaker.shared.game.config

import com.neo.sk.breaker.shared.model.Point

/**
  * Created by sky
  * Date on 2019/2/13
  * Time at 下午4:15
  */
trait BreakGameConfig {
  val esRecoverSupport: Boolean = true

  val totalRow = 15

  val brickRow = 7

  val totalColumn = 6

  val brickColumn = 4

  val frameDuration: Long

  val ballSpeed: Point

  val ballMaxFly: Byte

  def getBallRadius(l: Byte): Float

  def getBallDamage(l: Byte): Int

  def getBallLevel(damage: Int): Byte

  def getBallMaxLevel(): Byte

  def getBallRadiusByDamage(d: Int): Float

  val boundary: Point

  val obstacleWidth: Float

  val obstacleWO: Float

  val propMaxBlood: Byte

  val propNum: Int

  val brickMaxBlood: Byte

  val brickNum: Int
}

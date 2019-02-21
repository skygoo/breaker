package com.neo.sk.breaker.shared.`object`

import com.neo.sk.breaker.shared.`object`
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
  def this(config: BreakGameConfig,state:BreakState){
    this(config,state.playerId,state.breakId,state.name,state.position)
  }
  override protected val height: Float = config.breakHeight
  override protected val width: Float = config.breakWidth
  override protected val collisionOffset: Float = config.obstacleWO

  val up = if(position.y>config.boundary.y/2) false else true
  protected var curBulletNum:Int=1
  protected var gunDirection:Float=0
  protected var bulletLevel:Byte=1 //子弹等级=1

  protected var expression:Option[(Long,Byte,Option[String])]=None

  def setExpression(f:Long,et:Byte,s:Option[String])= expression=Some((f,et,s))

  def getExpression= expression

  // 获取坦克状态
  def getBreakState():BreakState = {
    BreakState(playerId,breakId,name,position)
  }

  def setTankGunDirection(a:Byte) = {
    val a_d=a.toDouble*3
    val theta=if(a<60){
      a_d*3.14/180
    }else{
      (360-a_d)*3.14/180
    }
    gunDirection = theta.toFloat
  }

  def setTankGunDirection(d:Float) = {
    gunDirection = d
  }

  // 获取发射子弹位置
  private def getLaunchBulletPosition():Point = {
    position + Point(config.breakGunWidth,0).rotate(gunDirection)
  }

  private def getTankBulletDamage():Int = {
    if(bulletLevel > 2) config.getBallDamage(2)
    else config.getBallDamage(bulletLevel)
  }

  def getBulletSize():Int = curBulletNum

  def launchBullet():Option[(Float,Point,Int)] = {
    if(curBulletNum > 0){
      curBulletNum = curBulletNum - 1
      Some(gunDirection,getLaunchBulletPosition(),getTankBulletDamage())
    }else None
  }

  def fillBullet()={
    curBulletNum+=1
  }

  def getPosition4Animation(offsetTime:Long) = {
    this.position
  }

  def getGunPositions4Animation(): List[Point] = {
    val gunWidth = config.breakGunWidth
    val gunHeight = config.breakGunHeight * (1 + (this.bulletLevel - 1) * 0.1f)
    List(
      Point(0, -gunHeight / 2).rotate(this.gunDirection),
      Point(0, gunHeight / 2).rotate(this.gunDirection),
      Point(gunWidth, gunHeight / 2).rotate(this.gunDirection),
      Point(gunWidth, -gunHeight / 2).rotate(this.gunDirection)
    )
  }
}

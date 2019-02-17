package com.neo.sk.breaker.shared.`object`

import com.neo.sk.breaker.shared.`object`
import com.neo.sk.breaker.shared.game.config.BreakGameConfig
import com.neo.sk.breaker.shared.model.{Point, Rectangle}

/**
  * Created by sky
  * Date on 2019/2/14
  * Time at 下午6:35
  * 发射球
  */
case class BallState(bId:Int, breakId:Int, position:Point, damage:Byte, momentum:Point)

case class Ball(
            config:BreakGameConfig,
            override protected var position: Point,
            damage:Int, //威力
            momentum:Point,
            bId:Int,
            breakId:Int
          ) extends CircleObjectOfGame {
  def this(config:BreakGameConfig, bulletState: BallState){
    this(config,bulletState.position,bulletState.damage.toInt,bulletState.momentum,bulletState.bId,bulletState.breakId)
  }

  var flyDecCount:Int=0
  //  val momentum: Point = momentum
  override val radius: Float = config.getBallRadiusByDamage(damage)

  val maxFly:Int = config.ballMaxFly

  // 获取子弹外形
  override def getObjectRect(): Rectangle = {
    Rectangle(this.position - Point(this.radius,this.radius),this.position + Point(this.radius, this.radius))
  }


  def getBulletState(): BallState = {
    `object`.BallState(bId,breakId,position,damage.toByte,momentum)
  }


  //子弹碰撞检测
  def isIntersectsObject(o: ObjectOfGame):Boolean = {
    this.isIntersects(o)
  }

  // 生命周期是否截至
  def isFlyEnd(boundary: Point):Boolean = {
    if( flyDecCount>config.ballMaxFly)
      true
    else
      false
  }

  // 先检测是否生命周期结束，如果没结束继续移动
  def move(boundary: Point,flyEndCallBack:Ball => Unit):Unit = {
    if(isFlyEnd(boundary)){
      flyEndCallBack(this)
    } else{
      this.position = this.position + momentum
    }
  }

  // 检测是否子弹有攻击到，攻击到，执行回调函数
  def checkAttackObject[T <: ObjectOfGame](o:T,attackCallBack:T => Unit):Unit = {
    if(this.isIntersects(o)){
      attackCallBack(o)
    }
  }


  def getPosition4Animation(offsetTime:Long) = {
    this.position + momentum / config.frameDuration * offsetTime
  }

  def getBulletLevel() = {
    config.getBallLevel(damage)
  }
}

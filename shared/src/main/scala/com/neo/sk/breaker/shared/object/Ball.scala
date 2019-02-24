package com.neo.sk.breaker.shared.`object`

import com.neo.sk.breaker.shared.`object`
import com.neo.sk.breaker.shared.game.config.BreakGameConfig
import com.neo.sk.breaker.shared.model.{Point, Rectangle}
import com.neo.sk.breaker.shared.util.QuadTree

/**
  * Created by sky
  * Date on 2019/2/14
  * Time at 下午6:35
  * 发射球
  */
case class BallState(bId: Int, breakId: Int, position: Point, damage: Byte, momentum: Point)

case class Ball(
                 config: BreakGameConfig,
                 override protected var position: Point,
                 damage: Int, //威力
                 var momentum: Point,
                 bId: Int,
                 breakId: Int
               ) extends CircleObjectOfGame {
  def this(config: BreakGameConfig, bulletState: BallState) {
    this(config, bulletState.position, bulletState.damage.toInt, bulletState.momentum, bulletState.bId, bulletState.breakId)
  }

  var flyDecCount: Int = 0
  override val radius: Float = config.getBallRadiusByDamage(damage)

  // 获取子弹外形
  override def getObjectRect(): Rectangle = {
    Rectangle(this.position - Point(this.radius, this.radius), this.position + Point(this.radius, this.radius))
  }


  def getBulletState(): BallState = {
    `object`.BallState(bId, breakId, position, damage.toByte, momentum)
  }

  // 生命周期是否截至
  def isFlyEnd(boundary: Point): Boolean = {
    if (flyDecCount > config.ballMaxFly || position.x <= 0 || position.y <= 0 || position.x >= boundary.x || position.y >= boundary.y)
      true
    else
      false
  }

  // 先检测是否生命周期结束，如果没结束继续移动
  def move(boundary: Point, quadTree: QuadTree, flyEndCallBack: Ball => Unit, attackCallBack: Obstacle => Unit): Unit = {
    if (isFlyEnd(boundary)) {
      flyEndCallBack(this)
    } else {
      val horizontalDistance = momentum.copy(y = 0)
      val verticalDistance = momentum.copy(x = 0)
      var count=true
      List(horizontalDistance, verticalDistance).foreach { d =>
        if (d.x != 0 || d.y != 0) {
          val originPosition = this.position
          this.position = this.position + d
          val otherObjects = quadTree.retrieveFilter(this).filter(_.isInstanceOf[ObstacleBall])
          val intList=otherObjects.filter(r => r.isIntersects(this)._1)
          if (intList.isEmpty) {
            quadTree.updateObject(this)
          }  else {
            intList.filter(r => r.isInstanceOf[Obstacle]).foreach { o =>
              flyDecCount += 1
              attackCallBack(o.asInstanceOf[Obstacle])
            }
            //fixme 此处存在BUg
            val h=intList.find(r => r.isIntersects(this)._2)
            if(h.nonEmpty&&count){
              count=false
              momentum = Point(-momentum.y,-momentum.x)
            }else if(count){
              if (d.x == 0) {
                momentum = momentum.copy(y = -momentum.y)
              }
              if (d.y == 0) {
                momentum = momentum.copy(x = -momentum.x)
              }
            }

            this.position = originPosition
          }
        }
      }
    }
  }

  def canMove(quadTree:QuadTree):Point = {
    var moveDistance = momentum
    val horizontalDistance = moveDistance.copy(y = 0)
    val verticalDistance = moveDistance.copy(x = 0)
    val originPosition = this.position
    List(horizontalDistance,verticalDistance).foreach{ d =>
      if(d.x != 0 || d.y != 0){
        val pos = this.position
        this.position = this.position + d
        val otherObjects = quadTree.retrieveFilter(this).filter(_.isInstanceOf[ObstacleBall])
        val intList=otherObjects.filter(r => r.isIntersects(this)._1)
        if (intList.isEmpty){

        }else{
          this.position = pos
          val h=intList.find(r => r.isIntersects(this)._2)
          if(h.nonEmpty){
            moveDistance -= d
          }
        }
      }
    }
    this.position = originPosition
    moveDistance
  }

  def getPosition4Animation(quadTree:QuadTree,offsetTime: Long) = {
    val logicMoveDistanceOpt = this.canMove(quadTree)
    this.position + logicMoveDistanceOpt / config.frameDuration * offsetTime
  }

  def getBulletLevel():Byte = {
    if(flyDecCount>config.ballMaxFly-3){
      2
    }else{
      1
    }
  }
}

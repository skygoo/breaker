package com.neo.sk.breaker.shared.`object`

import java.awt.event.KeyEvent

import com.neo.sk.breaker.shared.{`object`, model}
import com.neo.sk.breaker.shared.game.config.BreakGameConfig
import com.neo.sk.breaker.shared.model.{Point, Rectangle}
import com.neo.sk.breaker.shared.util.QuadTree

/**
  * Created by sky
  * Date on 2019/2/15
  * Time at 下午2:33
  * 游戏角色
  * 左右移动
  *
  */
case class BreakState(playerId: String, breakId: Int, name: String,width:Float,height:Float, position: Point)

case class Breaker(
                    config: BreakGameConfig,
                    playerId: String,
                    breakId: Int,
                    name: String,
                    width:Float,
                    height:Float,
                    override protected var position: Point
                  )extends RectangleObjectOfGame  with ObstacleBreak {
  def this(config: BreakGameConfig,state:BreakState){
    this(config,state.playerId,state.breakId,state.name,state.width,state.height,state.position)
  }

  override val pos: Byte = 0
  override protected val collisionOffset: Float = config.obstacleWO

  val up = if(position.y>config.boundary.y/2) false else true
  protected var curBulletNum:Int=1
  protected var gunDirection:Float=0
  protected var bulletLevel:Byte=1 //子弹等级=1

  protected var momentum:Point=Point(0,0)
  protected var isMove: Boolean=false

  protected var expression:Option[(Long,Byte,Option[String])]=None

  def setExpression(f:Long,et:Byte,s:Option[String])= expression=Some((f,et,s))

  def getExpression= expression

  // 获取坦克状态
  def getBreakState():BreakState = {
    BreakState(playerId,breakId,name,width,height,position)
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
    if(curBulletNum< config.breakBallLimit) {
      curBulletNum+=1
    }
  }

  def getPosition4Animation(boundary: Point, quadTree: QuadTree, offsetTime:Long) = {
    val logicMoveDistanceOpt = this.canMove(boundary, quadTree)
    if (logicMoveDistanceOpt.nonEmpty) {
      this.position + logicMoveDistanceOpt.get / config.frameDuration * offsetTime
    } else position
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

  def move(boundary: Point,quadTree: QuadTree):Unit = {
    if(isMove) {
      val horizontalDistance = momentum.copy(y = 0)
      val verticalDistance = momentum.copy(x = 0)
      List(horizontalDistance, verticalDistance).foreach { d =>
        if (d.x != 0 || d.y != 0) {
          val originPosition = this.position
          this.position = this.position + d
          val movedRec = Rectangle(this.position - Point(width/2, height/2), this.position + Point(width/2, height/2))
          val otherObjects = quadTree.retrieveFilter(this).filter(_.isInstanceOf[ObstacleBreak])
          if (!otherObjects.exists(t => t.isIntersects(this)._1) && movedRec.topLeft > model.Point(0, 0) && movedRec.downRight < boundary) {
            quadTree.updateObject(this)
          } else {
            this.position = originPosition
          }
        }
      }
    }
  }

  def canMove(boundary:Point, quadTree:QuadTree):Option[Point] = {
    if(isMove){
      var moveDistance = momentum
      val horizontalDistance = moveDistance.copy(y = 0)
      val verticalDistance = moveDistance.copy(x = 0)
      val originPosition = this.position
      List(horizontalDistance,verticalDistance).foreach{ d =>
        if(d.x != 0 || d.y != 0){
          val pos = this.position
          this.position = this.position + d
          val movedRec = Rectangle(this.position - Point(width/2, height/2), this.position + Point(width/2, height/2))
          val otherObjects = quadTree.retrieveFilter(this).filter(_.isInstanceOf[ObstacleBreak])
          if (!otherObjects.exists(t => t.isIntersects(this)._1) && movedRec.topLeft > model.Point(0, 0) && movedRec.downRight < boundary) {

          }else{
            this.position = pos
            moveDistance -= d
          }
        }
      }
      this.position = originPosition
      Some(moveDistance)
    }else{
      None
    }
  }

  /**
    * 根据坦克的按键修改坦克的方向状态
    * */
  def setTankDirection(actionSet:Set[Byte]) = {
    val targetDirectionOpt = getDirection(actionSet)
    if(targetDirectionOpt.nonEmpty) {
      isMove = true
      this.momentum = targetDirectionOpt.get
    } else isMove = false
  }

  import scala.language.implicitConversions
  protected final def getDirection(actionSet:Set[Byte]):Option[Point] = {
    implicit def changeInt2Byte(i:Int):Byte=i.toByte
    if(actionSet.contains(KeyEvent.VK_RIGHT)){
      Some(Point(config.breakSpeed,0))
    }else if(actionSet.contains(KeyEvent.VK_LEFT)){
      Some(Point(-config.breakSpeed,0))
    }else None
  }
}

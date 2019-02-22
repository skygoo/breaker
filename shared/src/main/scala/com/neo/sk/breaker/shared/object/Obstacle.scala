package com.neo.sk.breaker.shared.`object`

import com.neo.sk.breaker.shared.game.config.BreakGameConfig
import com.neo.sk.breaker.shared.model.Constants.ObstacleType
import com.neo.sk.breaker.shared.model.Point

/**
  * Created by hongruying on 2018/7/9
  * 游戏中的打击物
  * 空投（包含随机道具）
  * 砖头（可被子弹打碎）
  */
case class ObstacleState(oId:Int,t:Byte,pos:Byte,pt:Option[Byte],b:Option[Int],p:Point)

/**
  * @author sky
  * 控制移动阻碍（墙，队友）*/
trait ObstacleBreak

trait ObstacleBall

trait Obstacle extends RectangleObjectOfGame{

  val oId:Int

  val obstacleType:Byte

  val propType:Option[Byte]=None

  def getObstacleState():ObstacleState

  def attackDamage(d:Int):Unit

  def isLived():Boolean

  final def isIntersectsObject(o:Seq[ObjectOfGame]):Boolean = {
    o.exists(t => t.isIntersects(this)._1)
  }

  def getCurBlood():Int

}

object Obstacle{
  def apply(config: BreakGameConfig, obstacleState: ObstacleState): Obstacle = obstacleState.t match {
    case ObstacleType.airDropBox => new PropBox(config,obstacleState)
    case ObstacleType.brick => new Brick(config,obstacleState)
    case ObstacleType.wall => new Wall(config,obstacleState)
  }
}

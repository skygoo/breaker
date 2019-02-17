package com.neo.sk.breaker.shared.game

import com.neo.sk.breaker.shared.`object`.{Ball, BreakState, Breaker, Obstacle}
import com.neo.sk.breaker.shared.game.config.BreakGameConfig
import com.neo.sk.breaker.shared.model.Constants.ObstacleType
import com.neo.sk.breaker.shared.model.{Point, Rectangle}
import com.neo.sk.breaker.shared.protocol.BreakerEvent
import com.neo.sk.breaker.shared.protocol.BreakerEvent._
import com.neo.sk.breaker.shared.util.QuadTree

import scala.collection.mutable

/**
  * Created by sky
  * Date on 2019/2/2
  * Time at 下午8:59
  * 实现游戏基类
  * 包含前后端游戏逻辑
  *
  *
  */
trait GameContainer {
  import scala.language.implicitConversions

  def debug(msg: String): Unit

  def info(msg: String): Unit

  implicit val config:BreakGameConfig

  val boundary : Point = config.boundary

  var systemFrame:Long = 0L //系统帧数
  protected var upLine=(config.totalRow-config.brickRow)/2+1
  protected var downLine=(config.totalRow+config.brickRow)/2

  val breakMap = mutable.HashMap[Int,Breaker]()
  val ballMap = mutable.HashMap[Int,Ball]() //bulletId -> Bullet
  val obstacleMap = mutable.HashMap[Int,Obstacle]() //obstacleId -> Obstacle  可打击的砖头
  val quadTree : QuadTree = new QuadTree(Rectangle(Point(0,0),boundary))

  protected val gameEventMap = mutable.HashMap[Long,List[GameEvent]]() //frame -> List[GameEvent] 待处理的事件 frame >= curFrame
  protected val actionEventMap = mutable.HashMap[Long,List[UserActionEvent]]() //frame -> List[UserActionEvent]

  protected final def addUserAction(action:UserActionEvent):Unit = {
    actionEventMap.get(action.frame) match {
      case Some(actionEvents) => actionEventMap.put(action.frame,action :: actionEvents)
      case None => actionEventMap.put(action.frame,List(action))
    }
  }

  protected final def addGameEvent(event:GameEvent):Unit = {
    gameEventMap.get(event.frame) match {
      case Some(events) => gameEventMap.put(event.frame, event :: events)
      case None => gameEventMap.put(event.frame,List(event))
    }
  }

  private implicit def state2Breaker(breaker:BreakState):Breaker={
    Breaker(config,breaker.playerId,breaker.breakId,breaker.name,breaker.position)
  }
  protected def handleUserJoinRoomEvent(e:UserJoinRoom) :Unit = {
    //    println(s"-------------------处理用户加入房间事件")
    val breaker : BreakState = e.tankState
    breakMap.put(e.tankState.breakId,breaker)
    quadTree.insert(breaker)
  }

  final protected def handleUserJoinRoomEvent(l:List[UserJoinRoom]) :Unit = {
    l foreach handleUserJoinRoomEvent
  }

  //服务器和客户端执行的逻辑不一致
  protected def handleUserJoinRoomEventNow() = {
    gameEventMap.get(systemFrame).foreach{ events =>
      handleUserJoinRoomEvent(events.filter(_.isInstanceOf[UserJoinRoom]).map(_.asInstanceOf[UserJoinRoom]).reverse)
    }
  }

  protected def handleUserLeftRoom(e:UserLeftRoom) :Unit = {
    info("game stop 4 userLeft")
  }

  final protected def handleUserLeftRoom(l:List[UserLeftRoom]) :Unit = {
    l foreach handleUserLeftRoom
  }

  final protected def handleUserLeftRoomNow() = {
    gameEventMap.get(systemFrame).foreach{ events =>
      handleUserLeftRoom(events.filter(_.isInstanceOf[UserLeftRoom]).map(_.asInstanceOf[UserLeftRoom]).reverse)
    }
  }

  protected def handleGenerateObstacle(e:GenerateObstacle) :Unit = {
    val obstacle = Obstacle(config,e.obstacleState)
    obstacleMap.put(obstacle.oId,obstacle)
    quadTree.insert(obstacle)
  }

  protected final def handleGenerateObstacle(es:List[GenerateObstacle]) :Unit = {
    es foreach handleGenerateObstacle
  }

  final protected def handleGenerateObstacleNow() = {
    gameEventMap.get(systemFrame).foreach{ events =>
      handleGenerateObstacle(events.filter(_.isInstanceOf[GenerateObstacle]).map(_.asInstanceOf[GenerateObstacle]).reverse)
    }
  }

  protected def handleObstacleRemove(e:ObstacleRemove) :Unit = {
    obstacleMap.get(e.obstacleId).foreach { obstacle =>
      quadTree.remove(obstacle)
      obstacleMap.remove(e.obstacleId)
    }
  }

  protected final def handleObstacleRemove(es:List[ObstacleRemove]) :Unit = {
    es foreach handleObstacleRemove
  }

  protected def handleObstacleRemoveNow()= {
    gameEventMap.get(systemFrame).foreach { events =>
      handleObstacleRemove(events.filter(_.isInstanceOf[ObstacleRemove]).map(_.asInstanceOf[ObstacleRemove]).reverse)
    }
  }

  //小球攻击到障碍物的回调函数，游戏后端需要重写,生成伤害事件
  protected def attackObstacleCallBack(ball: Ball)(o:Obstacle):Unit = {
    val event = BreakerEvent.ObstacleAttacked(o.oId,ball.bId,ball.damage,systemFrame)
    addGameEvent(event)
  }

  protected final def removeBall(ball: Ball):Unit = {
    ballMap.remove(ball.bId)
    quadTree.remove(ball)
  }

  protected def ballMove():Unit = {
    ballMap.toList.sortBy(_._1).map(_._2).foreach{ ball =>
      val objects = quadTree.retrieveFilter(ball)
      objects.filter(t => t.isInstanceOf[Obstacle]).map(_.asInstanceOf[Obstacle])
        .foreach(t => ball.checkAttackObject(t,attackObstacleCallBack(ball)))
      ball.move(boundary,removeBall)

    }
  }

  def update():Unit = {
    handleUserLeftRoomNow()

  }

  protected def clearEventWhenUpdate():Unit

  def getGameContainerAllState():GameContainerAllState = {
    GameContainerAllState(
      systemFrame
    )
  }
}

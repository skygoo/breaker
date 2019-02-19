package com.neo.sk.breaker.shared.game

import com.neo.sk.breaker.shared.`object`._
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
  protected var upLine=(config.totalWallRow-config.brickRow)/2
  protected var downLine=(config.totalWallRow+config.brickRow)/2

  val breakMap = mutable.HashMap[Int,Breaker]()
  val ballMap = mutable.HashMap[Int,Ball]() //bulletId -> Bullet
  val obstacleMap = mutable.HashMap[Int,Obstacle]() //obstacleId -> Obstacle  可打击的砖头
  val environmentMap = mutable.HashMap[Int,Obstacle]() //obstacleId -> steel and river  不可打击
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

  //fixme 前后端不同的执行
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

  /**
    * 服务器和客户端执行的逻辑不一致
    * 服务器需要进行子弹容量计算，子弹生成事件，
    * 客户端只需要进行子弹容量计算
    * */
  protected def tankExecuteLaunchBulletAction(breaker:Breaker) : Unit

  protected final def handleUserActionEvent(actions:List[UserActionEvent]) = {
    /**
      * 用户行为事件
      * */
    actions.sortBy(t => (t.breakId,t.serialNum)).foreach{ action =>
      breakMap.get(action.breakId) match {
        case Some(breaker) =>
          action match {
            case a:UserMouseMove =>
              breaker.setTankGunDirection(a.d)
            case a:UserMouseClick =>
              //remind 调整鼠标方向
              breaker.setTankGunDirection(a.d)
              tankExecuteLaunchBulletAction(breaker)
          }
        case None => info(s"breakerId=${action.breakId} action=${action} is no valid,because the breaker is not exist")
      }
    }
  }

  final protected def handleUserActionEventNow() = {
    actionEventMap.get(systemFrame).foreach{ actionEvents =>
      handleUserActionEvent(actionEvents.reverse)
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

  protected def handleObstacleAttacked(e:ObstacleAttacked) :Unit = {
   obstacleMap.get(e.obstacleId).foreach{ obstacle =>
      if(obstacle.isLived()){
        obstacle.attackDamage(e.damage)
      }
    }
  }

  protected final def handleObstacleAttacked(es:List[ObstacleAttacked]) :Unit = {
    es foreach handleObstacleAttacked
  }

  final protected def handleObstacleAttackedNow() = {
    gameEventMap.get(systemFrame).foreach{ events =>
      handleObstacleAttacked(events.filter(_.isInstanceOf[ObstacleAttacked]).map(_.asInstanceOf[ObstacleAttacked]).reverse)
    }
  }

  protected def handleGenerateBall(e:GenerateBall) :Unit = {
    //客户端和服务端重写
    val ball = new Ball(config,e.ball)
    ballMap.put(e.ball.bId,ball)
    quadTree.insert(ball)
  }

  protected final def handleGenerateBullet(es:List[GenerateBall]) :Unit = {
    es foreach handleGenerateBall
  }

  final protected def handleGenerateBulletNow() = {
    gameEventMap.get(systemFrame).foreach{ events =>
      handleGenerateBullet(events.filter(_.isInstanceOf[GenerateBall]).map(_.asInstanceOf[GenerateBall]).reverse)
    }
  }

  def update():Unit = {
    handleUserLeftRoomNow()
    ballMove()

    handleUserActionEventNow()
    handleObstacleAttackedNow()

    handleObstacleRemoveNow()
    handleGenerateObstacleNow()

    handleGenerateBulletNow()
    quadTree.refresh(quadTree)

    clearEventWhenUpdate()
  }

  protected def clearEventWhenUpdate():Unit

  def getGameContainerAllState():GameContainerAllState = {
    GameContainerAllState(
      systemFrame,
      breakMap.values.map(_.getBreakState()).toList,
      ballMap.values.map(_.getBulletState()).toList,
      obstacleMap.values.map(_.getObstacleState()).toList,
      environmentMap.values.map(_.getObstacleState()).toList
    )
  }

  def getGameContainerState():GameContainerState = {
    GameContainerState(
      systemFrame
    )
  }
}

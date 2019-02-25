package com.neo.sk.breaker.shared.game

import com.neo.sk.breaker.shared.`object`._
import com.neo.sk.breaker.shared.game.config.BreakGameConfig
import com.neo.sk.breaker.shared.model.Constants.{ObstacleType, PropType}
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

  var systemFrame:Int = 0 //系统帧数

  val breakMap = mutable.HashMap[Boolean,Breaker]()
  val ballMap = mutable.HashMap[Int,Ball]() //bulletId -> Bullet
  val obstacleMap = mutable.HashMap[Int,Obstacle]() //obstacleId -> Obstacle  可打击的砖头
  val environmentMap = mutable.HashMap[Int,Obstacle]() //obstacleId -> steel and river  不可打击
  val quadTree : QuadTree = new QuadTree(Rectangle(Point(0,0),boundary))

  val breakMoveAction = mutable.HashMap[Boolean,mutable.HashSet[Byte]]() //tankId -> pressed direction key code

  protected val gameEventMap = mutable.HashMap[Long,List[GameEvent]]() //frame -> List[GameEvent] 待处理的事件 frame >= curFrame
  protected val actionEventMap = mutable.HashMap[Long,List[UserActionEvent]]() //frame -> List[UserActionEvent]

  var winner:(String,String,Boolean,Int)=("","",false,0)

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

  implicit def state2Breaker(breaker:BreakState):Breaker={
    new Breaker(config,breaker)
  }

  protected def handleUserLeftRoom(e:UserLeftRoom) :Unit = {
    info("game stop 4 userLeft")
    breakMap.find(_._1 != e.breakId).foreach{ b=>
      val event=BreakerEvent.GameOver(b._2.breakId,systemFrame)
      addGameEvent(event)
    }
  }

  final protected def handleUserLeftRoom(l:List[UserLeftRoom]) :Unit = {
    l foreach handleUserLeftRoom
  }

  final protected def handleUserLeftRoomNow() = {
    gameEventMap.get(systemFrame).foreach{ events =>
      handleUserLeftRoom(events.filter(_.isInstanceOf[UserLeftRoom]).map(_.asInstanceOf[UserLeftRoom]).reverse)
    }
  }

  protected def handleGameOver(e:GameOver) :Unit={
    breakMap.find(_._1==e.breakId).foreach(r=>winner=(r._2.playerId,r._2.name,r._2.breakId,r._2.crashCount))
  }

  final protected def handleGameOver(l:List[GameOver]) :Unit = {
    l foreach handleGameOver
  }

  final protected def handleGameOverNow()={
    gameEventMap.get(systemFrame).foreach{ events =>
      handleGameOver(events.filter(_.isInstanceOf[GameOver]).map(_.asInstanceOf[GameOver]).reverse)
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
      val tankMoveSet = breakMoveAction.getOrElse(action.breakId,mutable.HashSet[Byte]())
      breakMap.get(action.breakId) match {
        case Some(breaker) =>
          action match {
            case a:UserMouseMove =>
              breaker.setTankGunDirection(a.d)
            case a:UserMouseClick =>
              //remind 调整鼠标方向
              breaker.setTankGunDirection(a.d)
              tankExecuteLaunchBulletAction(breaker)

            case a:UserPressKeyDown =>
              tankMoveSet.add(a.keyCodeDown)
              breakMoveAction.put(a.breakId,tankMoveSet)
              breaker.setTankDirection(tankMoveSet.toSet)

            case a:UserPressKeyUp =>
              tankMoveSet.remove(a.keyCodeUp)
              breakMoveAction.put(a.breakId,tankMoveSet)
              breaker.setTankDirection(tankMoveSet.toSet)

            case e:Expression=>
              breakMap.get(e.breakId).foreach(b=>b.setExpression(e.frame,e.et,e.s))
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
  protected def attackObstacleCallBack(ball: Ball)(t:Boolean,o:Int):Unit = {
    if(t){
      val event = BreakerEvent.ObstacleAttacked(o,ball.breakId,ball.bId,ball.damage,systemFrame)
      addGameEvent(event)
    }else{
      val event = BreakerEvent.BreakAttacked(if(o==0) true else false,ball.bId,systemFrame)
      addGameEvent(event)
    }
  }

  protected final def removeBall(ball: Ball):Unit = {
    ballMap.remove(ball.bId)
    quadTree.remove(ball)
  }

  protected def ballMove():Unit = {
    ballMap.toList.sortBy(_._1).map(_._2).foreach{ ball =>
      ball.move(boundary,quadTree,removeBall,attackObstacleCallBack(ball))
    }
  }

  protected def breakMove():Unit = {
    breakMap.toList.sortBy(_._1).map(_._2).foreach{ ball =>
      ball.move(boundary,quadTree)
    }
  }

  protected def objectMove():Unit = {
    breakMove()
    ballMove()
  }

  protected def handleObstacleAttacked(e:ObstacleAttacked) :Unit

  protected final def handleObstacleAttacked(es:List[ObstacleAttacked]) :Unit = {
    es foreach handleObstacleAttacked
  }

  final protected def handleObstacleAttackedNow() = {
    gameEventMap.get(systemFrame).foreach{ events =>
      handleObstacleAttacked(events.filter(_.isInstanceOf[ObstacleAttacked]).map(_.asInstanceOf[ObstacleAttacked]).reverse)
    }
  }

  protected def handleBreakAttacked(e:BreakAttacked) :Unit ={
    ballMap.filter(_._1==e.ballId).foreach(_._2.breakId=e.breakId)
  }

  protected final def handleBreakAttacked(es:List[BreakAttacked]) :Unit = {
    es foreach handleBreakAttacked
  }

  final protected def handleBreakAttackedNow() = {
    gameEventMap.get(systemFrame).foreach{ events =>
      handleBreakAttacked(events.filter(_.isInstanceOf[BreakAttacked]).map(_.asInstanceOf[BreakAttacked]).reverse)
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

  final protected def handleFillBulletNow()={
    if(systemFrame%config.fillBallFrame==0){
      breakMap.foreach(_._2.fillBullet())
    }
  }

  def update():Unit = {
    handleUserLeftRoomNow()
    handleGameOverNow()
    objectMove()
    handleUserActionEventNow()
    handleObstacleAttackedNow()
    handleBreakAttackedNow()
    handleObstacleRemoveNow()
    handleGenerateObstacleNow()

    handleGenerateBulletNow()

    handleFillBulletNow()

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

  def getGameContainerState(s:Boolean):GameContainerState = {
    GameContainerState(
      systemFrame,
      if(s) Some(breakMap.values.map(_.getBreakState()).toList) else None,
    )
  }
}

package com.neo.sk.breaker.core.game

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.ActorRef
import com.neo.sk.breaker.core.{RoomActor, UserActor}
import com.neo.sk.breaker.protocol.ActorProtocol
import com.neo.sk.breaker.shared.`object`._
import com.neo.sk.breaker.shared.game.GameContainer
import com.neo.sk.breaker.shared.game.config.{BreakGameConfig, BreakGameConfigImpl}
import com.neo.sk.breaker.shared.model.Constants.{ObstacleType, PropType}
import com.neo.sk.breaker.shared.model.{Constants, Point}
import com.neo.sk.breaker.shared.protocol.BreakerEvent
import com.neo.sk.breaker.shared.protocol.BreakerEvent.{GameOver, UserJoinRoom, UserLeftRoom}
import org.slf4j.Logger

import scala.util.Random
import scala.collection.mutable
/**
  * Created by sky
  * Date on 2019/2/14
  * Time at 下午6:30
  */
case class GameContainerServerImpl(
                                    config:BreakGameConfig,
                                    log: Logger,
                                    roomActorRef: ActorRef[RoomActor.Command],
                                    dispatch: BreakerEvent.WsMsgServer => Unit,
                                  ) extends GameContainer{

  def debug(msg: String)=log.debug(msg)

  def info(msg: String): Unit=log.info(msg)

  private val ballIdGenerator = new AtomicInteger(100)
  private val obstacleIdGenerator = new AtomicInteger(100)
  private val random = new Random(System.currentTimeMillis())

  //remind 用于生成新行
  protected var upLine=(config.totalWallRow-config.brickRow)/2
  protected var downLine=(config.totalWallRow+config.brickRow)/2-1
  private val positionList=mutable.HashMap[Int,List[(Int,Byte)]]()
  private var justJoinUser: List[(String, String,Int, ActorRef[UserActor.Command],ActorRef[BreakerEvent.WsMsgSource])] = Nil // tankIdOpt


  val xPosition=boundary.x/(config.totalColumn+4)
  val yPosition=boundary.y/(config.totalWallRow+4)

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

  private def genObstaclePositionRandom(row:Int): List[(Int,Point)] = {
    val value=mutable.HashSet[Int]()
    for(i <- config.brickColumn until config.totalColumn){
      var n=random.nextInt(config.totalColumn)
      while (value.contains(n)){
        n=random.nextInt(config.totalColumn)
      }
      value.add(n)
    }
    val pList=mutable.HashSet[(Int,Point)]()
    for(i <- 0 until config.totalColumn){
      if(!value.contains(i)){
        pList.add(row,Point(xPosition*(2+i)+config.obstacleWidth/2,yPosition*(2+row)+config.obstacleWidth/2))
      }
    }
    pList.toList
  }

  private def generateAirDrop(position:Point) = {
    val oId = obstacleIdGenerator.getAndIncrement()
    PropBox(config, oId, position, random.nextInt(config.propMaxBlood)+1, Some(random.nextInt(Constants.PropType.typeSize).toByte))
  }

  private def generateBrick(position:Point) = {
    val oId = obstacleIdGenerator.getAndIncrement()
    Brick(config, oId, position, random.nextInt(config.propMaxBlood)+1)
  }

  private def generateWall(position:Point) = {
    val oId = obstacleIdGenerator.getAndIncrement()
    Wall(config, oId, position)
  }

  private def generateObstacle(plist:List[(Int,Point)]) = {
    val pList=mutable.HashSet[(Int,Point)]()
    plist.foreach(l=>pList.add(l))
    val propSize=random.nextInt(3)
    (1 until propSize).foreach{_=>
      val list=pList.toList
      val p=list(random.nextInt(list.size))
      val obstacle = generateAirDrop(p._2)
      pList.remove(p)
      val event = BreakerEvent.GenerateObstacle(systemFrame+1, obstacle.getObstacleState())
      addGameEvent(event)
      dispatch(event)
      positionList.get(p._1) match {
        case Some(value)=>
          positionList.update(p._1,(obstacle.oId,obstacle.obstacleType)::value)
        case None=>
          positionList.put(p._1,List((obstacle.oId,obstacle.obstacleType)))
      }
    }
    pList.foreach { p =>
      val obstacle = generateBrick(p._2)
      val event = BreakerEvent.GenerateObstacle(systemFrame+1, obstacle.getObstacleState())
      addGameEvent(event)
      dispatch(event)
      positionList.get(p._1) match {
        case Some(value)=>
          positionList.update(p._1,(obstacle.oId,obstacle.obstacleType)::value)
        case None=>
          positionList.put(p._1,List((obstacle.oId,obstacle.obstacleType)))
      }
    }
  }

  private def initObstacle(pList:mutable.HashSet[(Int,Point)]) = {
    (1 to config.propNum).foreach { _ =>
      val list=pList.toList
      val p=list(random.nextInt(list.size))
      val obstacle = generateAirDrop(p._2)
      pList.remove(p)
      positionList.get(p._1) match {
        case Some(value)=>
          positionList.update(p._1,(obstacle.oId,obstacle.obstacleType)::value)
        case None=>
          positionList.put(p._1,List((obstacle.oId,obstacle.obstacleType)))
      }
      obstacleMap.put(obstacle.oId, obstacle)
      quadTree.insert(obstacle)
    }
    pList.foreach { p =>
      val obstacle = generateBrick(p._2)
      val event = BreakerEvent.GenerateObstacle(systemFrame, obstacle.getObstacleState())
      addGameEvent(event)
      positionList.get(p._1) match {
        case Some(value)=>
          positionList.update(p._1,(obstacle.oId,obstacle.obstacleType)::value)
        case None=>
          positionList.put(p._1,List((obstacle.oId,obstacle.obstacleType)))
      }
      obstacleMap.put(obstacle.oId, obstacle)
      quadTree.insert(obstacle)
    }
  }

  private def initEnvironment()={
    for(i <- 2 until config.totalWallRow+2){
      var obstacle = generateWall(Point(config.obstacleWidth/2,yPosition*i+config.obstacleWidth/2))
      environmentMap.put(obstacle.oId, obstacle)
      quadTree.insert(obstacle)
      obstacle = generateWall(Point(boundary.x-config.obstacleWidth/2,yPosition*i+config.obstacleWidth/2))
      environmentMap.put(obstacle.oId, obstacle)
      quadTree.insert(obstacle)
    }
  }

  private def init()={
    val pList=mutable.HashSet[(Int,Point)]()
    for(i <- upLine to downLine){
      genObstaclePositionRandom(i).foreach(p=>pList.add(p))
    }
    initObstacle(pList)
    initEnvironment()
  }

  /**初始化环境*/
  init()

  def joinGame(playerId: String, name: String, breakId:Int, userActor: ActorRef[UserActor.Command], frontActor:ActorRef[BreakerEvent.WsMsgSource]): Unit = {
    justJoinUser = (playerId, name, breakId,userActor, frontActor) :: justJoinUser
  }

  def receiveUserAction(preExecuteUserAction: BreakerEvent.UserActionEvent): Unit = {
    val f = math.max(preExecuteUserAction.frame, systemFrame)
    if (preExecuteUserAction.frame != f) {
      log.debug(s"preExecuteUserAction frame=${preExecuteUserAction.frame}, systemFrame=${systemFrame}")
    }
    /**
      * gameAction,userAction
      * 新增按键操作，补充血量，
      **/
    val action = preExecuteUserAction match {
      case a: BreakerEvent.UserMouseClick =>
        addUserAction(a.copy(frame = f))
        a.copy(frame = f)
      case a: BreakerEvent.Expression =>
        a.copy(frame = f)
    }

    dispatch(action)
  }

  private def genABreaker(playerId:String,breakId:Int,nickName:String,up:Boolean)={
    val position=if(up){
      Point(boundary.x.toInt/2,yPosition)
    }else{
      Point(boundary.x.toInt/2,boundary.y-yPosition)
    }
    val event = BreakerEvent.UserJoinRoom(BreakState(playerId,breakId,nickName,position), systemFrame)
    dispatch(event)
    addGameEvent(event)
  }

  def startGame={
    if(justJoinUser.size==2){
      var user=justJoinUser.head
      genABreaker(user._1,user._3,user._2,true)
      user=justJoinUser.reverse.head
      genABreaker(user._1,user._3,user._2,false)
    }
    handleUserJoinRoomEventNow()
    breakMap.foreach{b=>
      justJoinUser.find(_._1==b._2.playerId) match {
        case Some(value)=>
          value._4 ! ActorProtocol.JoinRoomSuccess(b._2,config.asInstanceOf[BreakGameConfigImpl],roomActorRef)
        case None=>
          log.error("user join error")
      }
    }
  }

  def leftGame(playerId: String) = {
    breakMap.find(_._2.playerId==playerId).foreach{r=>
      log.info(s"bot/user leave $playerId")
      val event = BreakerEvent.UserLeftRoom(playerId, r._2.name, r._2.breakId, systemFrame)
      addGameEvent(event)
      dispatch(event)
    }
  }

  override protected def handleGameOver(e: GameOver): Unit = {
    super.handleGameOver(e)
    roomActorRef ! ActorProtocol.GameOver
  }

  override protected def handleGenerateObstacle(e: BreakerEvent.GenerateObstacle): Unit = {
    val obstacle = Obstacle(config, e.obstacleState)
    val originObstacleOpt = if (e.obstacleState.t == ObstacleType.wall) environmentMap.put(obstacle.oId, obstacle)
    else obstacleMap.put(obstacle.oId, obstacle)
    if (originObstacleOpt.isEmpty) {
      quadTree.insert(obstacle)
    } else {
      quadTree.remove(originObstacleOpt.get)
      quadTree.insert(obstacle)
    }
  }

  override protected def handleObstacleAttacked(e: BreakerEvent.ObstacleAttacked): Unit = {
    obstacleMap.get(e.obstacleId).foreach { obstacle =>
      if(obstacle.obstacleType==ObstacleType.airDropBox){
        obstacle.propType.foreach {
          case PropType.addBallProp =>
            breakMap.get(e.breakId) match {
              case Some(value)=>
                value.fillBullet()
              case None=>
            }
          case PropType.decBallProp =>
            ballMap.get(e.ballId) match {
              case Some(value)=>
                removeBall(value)
              case None=>
            }
        }
      }
      obstacle.attackDamage(e.damage)
      if (!obstacle.isLived()) {
        //砖块消失
        val event = BreakerEvent.ObstacleRemove(e.obstacleId,e.breakId,e.ballId,systemFrame)
        dispatch(event)
        addGameEvent(event)

        positionList.filter(_._2.map(_._1).contains(e.obstacleId)).foreach(h=>
          positionList.update(h._1,h._2.filterNot(_._1==e.obstacleId))
        )
      }
    }
  }

  protected def handleGenerateBrickNow()={
    positionList.get(upLine) match {
      case Some(value)=>
        if(value.isEmpty){
          positionList.remove(upLine)
          upLine+=1
          downLine+=1
          if(downLine>=config.totalWallRow){
            breakMap.find(_._2.up).foreach{ b=>
              val event=BreakerEvent.GameOver(b._2.breakId,systemFrame+1)
              addGameEvent(event)
              dispatch(event)
            }
          }else{
            generateObstacle(genObstaclePositionRandom(downLine))
          }
        }
      case None=>
        upLine+=1
        downLine+=1
        if(downLine>=config.totalWallRow){
          breakMap.find(_._2.up).foreach{ b=>
            val event=BreakerEvent.GameOver(b._2.breakId,systemFrame+1)
            addGameEvent(event)
            dispatch(event)
          }
        }else{
          generateObstacle(genObstaclePositionRandom(downLine))
        }
    }
    positionList.get(downLine) match {
      case Some(value)=>
        if(value.isEmpty){
          positionList.remove(downLine)
          upLine-=1
          downLine-=1
          if(upLine<0){
            breakMap.find(!_._2.up).foreach{ b=>
              val event=BreakerEvent.GameOver(b._2.breakId,systemFrame+1)
              addGameEvent(event)
              dispatch(event)
            }
          }else{
            generateObstacle(genObstaclePositionRandom(upLine))
          }
        }
      case None=>
        upLine-=1
        downLine-=1
        if(upLine<0){
          breakMap.find(!_._2.up).foreach{ b=>
            val event=BreakerEvent.GameOver(b._2.breakId,systemFrame+1)
            addGameEvent(event)
            dispatch(event)
          }
        }else{
          generateObstacle(genObstaclePositionRandom(upLine))
        }
    }
  }


  override def tankExecuteLaunchBulletAction(breaker: Breaker): Unit = {
    def transformGenerateBulletEvent(ballState: BallState, s:Boolean=true) = {
      val event = BreakerEvent.GenerateBall(systemFrame, ballState, s)
      dispatch(event)
      addGameEvent(event)
    }
    breaker.launchBullet() match {
      case Some((bulletDirection, position, damage)) =>
        val momentum = config.ballSpeed.rotate(bulletDirection) * config.frameDuration / 1000
        val ballState = BallState(ballIdGenerator.getAndIncrement(), breaker.breakId, position, damage.toByte, momentum)
        transformGenerateBulletEvent(ballState)
      case None => debug(s"breakerId=${breaker.breakId} has no bullet now")
    }
  }

  override protected def clearEventWhenUpdate(): Unit = {
    handleGenerateBrickNow()
    gameEventMap -= systemFrame - 1
    actionEventMap -= systemFrame - 1
    systemFrame += 1
  }



}

package com.neo.sk.breaker.core.game

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.ActorRef
import com.neo.sk.breaker.core.{RoomActor, UserActor}
import com.neo.sk.breaker.shared.`object`.{BreakState, Brick, PropBox}
import com.neo.sk.breaker.shared.game.GameContainer
import com.neo.sk.breaker.shared.game.config.{BreakGameConfig, BreakGameConfigImpl}
import com.neo.sk.breaker.shared.model.{Constants, Point}
import com.neo.sk.breaker.shared.protocol.BreakerEvent
import com.neo.sk.breaker.shared.protocol.BreakerEvent.UserLeftRoom
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

  private val positionList=mutable.HashMap[Int,List[Int]]()
  private var justJoinUser: List[(String, String,Int, ActorRef[UserActor.Command],ActorRef[BreakerEvent.WsMsgSource])] = Nil // tankIdOpt


  val xPosition=boundary.x/(config.totalColumn+4)
  val yPosition=boundary.y/(config.totalRow+4)

  private def genObstaclePositionRandom(row:Int): List[Point] = {
    val value=mutable.HashSet[Int]()
    for(i <- config.brickColumn to config.totalColumn){
      var n=random.nextInt(config.totalColumn)
      while (value.contains(n)){
        n=random.nextInt(config.totalColumn)
      }
      value.add(n)
    }
    val pList=mutable.HashSet[Point]()
    for(i <- 0 to config.totalColumn){
      if(!value.contains(i)){
        pList.add(Point(xPosition*(2+i),yPosition*(2+row)))
      }
    }
    pList.toList
  }

  private def generateAirDrop(position:Point) = {
    val oId = obstacleIdGenerator.getAndIncrement()
    PropBox(config, oId, position, random.nextInt(config.propMaxBlood)+1, random.nextInt(Constants.PropType.typeSize).toByte)
  }

  private def generateBrick(position:Point) = {
    val oId = obstacleIdGenerator.getAndIncrement()
    Brick(config, oId, position, random.nextInt(config.propMaxBlood)+1)
  }

  private def initObstacle(pList:mutable.HashSet[Point]) = {
    (1 to config.propNum).foreach { _ =>
      val list=pList.toList
      val position=list(random(list.size))
      val obstacle = generateAirDrop(position)
      pList.remove(position)
      val event = BreakerEvent.GenerateObstacle(systemFrame, obstacle.getObstacleState())
      addGameEvent(event)
      obstacleMap.put(obstacle.oId, obstacle)
      quadTree.insert(obstacle)
    }
    pList.foreach { p =>
      val obstacle = generateBrick(p)
      val event = BreakerEvent.GenerateObstacle(systemFrame, obstacle.getObstacleState())
      addGameEvent(event)
      obstacleMap.put(obstacle.oId, obstacle)
      quadTree.insert(obstacle)
    }
  }

  private def init()={
    val pList=mutable.HashSet[Point]()
    for(i <- upLine to downLine){
      genObstaclePositionRandom(i).foreach(p=>pList.add(p))
    }
    initObstacle(pList)
  }

  /**初始化环境*/
  init()

  def joinGame(playerId: String, name: String, breakId:Int, userActor: ActorRef[UserActor.Command], frontActor:ActorRef[BreakerEvent.WsMsgSource]): Unit = {
    justJoinUser = (playerId, name, breakId,userActor, frontActor) :: justJoinUser
  }

  private def genABreaker(playerId:String,breakId:Int,nickName:String,up:Boolean)={
    val position=if(up){
      Point(boundary.x.toInt/2,0)
    }else{
      Point(boundary.x.toInt/2,boundary.y)
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
    justJoinUser=Nil
  }

  def leftGame(playerId: String, name: String, breakId: Int) = {
    log.info(s"bot/user leave $playerId")
    val event = BreakerEvent.UserLeftRoom(playerId, name, breakId, systemFrame)
    addGameEvent(event)
    dispatch(event)
  }

  override protected def handleUserLeftRoom(e: UserLeftRoom): Unit = {
    super.handleUserLeftRoom(e)
    roomActorRef ! RoomActor.GameStopRoom
  }

  override protected def clearEventWhenUpdate(): Unit = {
    gameEventMap -= systemFrame - 1
    actionEventMap -= systemFrame - 1
    systemFrame += 1
  }



}

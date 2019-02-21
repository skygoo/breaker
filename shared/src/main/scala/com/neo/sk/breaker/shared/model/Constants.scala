package com.neo.sk.breaker.shared.model

import scala.util.Random

/**
  * Created by sky
  * Date on 2019/2/14
  * Time at 下午6:26
  */
object Constants {

  object DirectionType {
    final val right:Float = 0
    final val left = math.Pi
  }

  object BreakColor{
    val blue = "#1E90FF"
    val green = "#4EEE94"
    val red = "#EE4000"
    val tankColorList = List(blue,green,red)
    val gun = "#7A7A7A"
    def getRandomColorType(random:Random):Byte = random.nextInt(tankColorList.size).toByte
  }

  object PropType{
    val typeSize=2
    val addBallProp:Byte=0
    val decBallProp:Byte=1
  }

  object ObstacleType{
    val airDropBox:Byte = 1
    val brick:Byte = 2
    val wall:Byte = 3
  }

  object GameAnimation{
    val bulletHitAnimationFrame = 8
    val tankDestroyAnimationFrame = 12
    val talkAnimationFrame = 20
  }


  val PreExecuteFrameOffset = 2 //预执行2帧
  val fakeRender = true

  object GameState{
    val firstCome = 1
    val play = 2
    val stop = 3
    val loadingPlay = 4
    val loadingWait = 5
    val leave = 6
  }

  object ExpressionMap{
    val e0:Byte=0
    val e1:Byte=1
    val e2:Byte=2
    val e3:Byte=3
    val e4:Byte=4
    val e5:Byte=5
    val list=List((e1,"哭"),(e2,"笑"),(e3,"生气"),(e4,"难受"),(e5,"高兴"))
  }

  object RoomType{
    val cooperation:Byte=1
    val confrontation:Byte=2
  }


  final val WindowView = Point(60,100)

}

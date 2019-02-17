package com.neo.sk.breaker.shared.model

import scala.util.Random

/**
  * Created by hongruying on 2018/8/28
  */
object Constants {

  val drawHistory = false

  object DirectionType {
    final val right:Float = 0
    final val left = math.Pi
  }

  object TankColor{
    val blue = "#1E90FF"
    val green = "#4EEE94"
    val red = "#EE4000"
    val tankColorList = List(blue,green,red)
    val gun = "#7A7A7A"
    def getRandomColorType(random:Random):Byte = random.nextInt(tankColorList.size).toByte

  }

  object InvincibleSize{
    val r = 5.5
  }

  object SmallBullet{
    val num = 4
    val height = 5
    val width = 1
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
  }

  object PropAnimation{
    val DisAniFrame1 = 30
    val DisplayF1 = 6
    val DisappearF1 = 2
    val DisAniFrame2 = 10
    val DisplayF2 = 1
    val DisappearF2 = 1
  }


  val PreExecuteFrameOffset = 2 //预执行2帧
  val fakeRender = true

  object GameState{
    val firstCome = 1
    val play = 2
    val stop = 3
    val loadingPlay = 4
    val replayLoading = 5
    val leave = 6
  }


  final val WindowView = Point(192,108)

}

package com.neo.sk.tank.test

import scala.util.Random

/**
  * Created by sky
  * Date on 2019/2/17
  * Time at 下午1:58
  */
object Test {
  val random = new Random(System.currentTimeMillis())

  def main(args: Array[String]): Unit = {
    for(i <- 1 to 100){
      println(random.nextInt(3))
    }
  }
}

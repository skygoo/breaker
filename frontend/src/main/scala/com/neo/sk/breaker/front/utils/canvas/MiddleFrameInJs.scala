package com.neo.sk.breaker.front.utils.canvas

import com.neo.sk.breaker.shared.util.canvas.{MiddleCanvas, MiddleFrame, MiddleImage}
import com.neo.sk.breaker.shared.util.canvas.{MiddleCanvas, MiddleImage}

/**
  * Created by sky
  * Date on 2018/11/17
  * Time at 上午11:23
  */
class MiddleFrameInJs extends MiddleFrame {
  override def createCanvas(width: Double, height: Double): MiddleCanvas = MiddleCanvasInJs(width, height)

  def createCanvas(name: String, width: Double, height: Double) = MiddleCanvasInJs(name, width, height)

  override def createImage(url: String): MiddleImage = MiddleImageInJs(url)
}

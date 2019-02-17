package com.neo.sk.breaker.front.components.model

import mhtml.{Rx, Var}

import scala.xml.{Elem, Node}

/**
  * Created by sky
  * Date on 2018/12/19
  * Time at 下午3:34
  * 模态框特征
  */
trait Model {
  val showQRCode = Var(false)

  val modelDiv:Elem

  def cancelShowQR = {
    showQRCode := false
  }

  val QRCodeBox:Rx[Node]
}

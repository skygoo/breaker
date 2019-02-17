package com.neo.sk.breaker.front.components.model

import mhtml.emptyHTML

/**
  * Created by sky
  * Date on 2018/12/19
  * Time at 下午3:38
  * 相对模态框
  */
abstract class RelativeModel(style:String) extends Model{
  val QRCodeBox = showQRCode.map {
    case true =>
      <div style={style} class="down-model">
        {modelDiv}
      </div>
    case false =>
      emptyHTML
  }
}

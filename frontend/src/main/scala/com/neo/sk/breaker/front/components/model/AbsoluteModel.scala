package com.neo.sk.breaker.front.components.model

import mhtml.emptyHTML
import org.scalajs.dom.Event

/**
  * Created by sky
  * Date on 2018/12/16
  * Time at 下午5:12
  * 绝对模态框
  */

abstract class AbsoluteModel(style:String="display:block;background:rgba(0,0,0,0.5);") extends Model {
  val QRCodeBox = showQRCode.map {
    show =>
      if (show) {
        <div class="modal show" onclick={() => showQRCode:=false} style={style}>
          <div class="width:100% height:100%" role="document" onclick={(e: Event) => e.stopPropagation()}>
            {
            modelDiv
            }
          </div>
        </div>
      }
      else {
        emptyHTML
      }
  }
}

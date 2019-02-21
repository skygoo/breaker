package com.neo.sk.breaker.front.components

import com.neo.sk.breaker.front.common.Routes
import com.neo.sk.breaker.front.components.model.AbsoluteModel
import mhtml.emptyHTML

/**
  * Created by sky
  * Date on 2018/12/28
  * Time at 下午3:03
  * 提示框和询问框
  */
class ConfirmModel(text:String, commitCallBack: => Unit, f:Boolean) extends AbsoluteModel{
  var returnFlag=false
  override val modelDiv =
    <div class={s"model-show create-modal-show"} style="width:448px;height:250px">
      <div class="model-title">
        <span class="model-title-l">注意</span>
        <img src={Routes.getImgUrl("ic-关闭.png")} class="model-title-r" onclick={() => cancelShowQR}></img>
      </div>
      <div class="model-body" style="height:60%;text-align: center; padding-top:10%;">
        <span style="font-size: 20px;">{text}</span>
      </div>
      <div class="model-bottom">
        {if(f) <button class="model-bottom-b model-bottom-n" onclick={() => showQRCode := false}>取 消</button> else emptyHTML}
        <button class="model-bottom-b model-bottom-y" onclick={() =>
          commitCallBack
          showQRCode:=false
        }>确 定</button>
      </div>
    </div>

}

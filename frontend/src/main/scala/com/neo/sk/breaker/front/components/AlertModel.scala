package com.neo.sk.breaker.front.components

import com.neo.sk.breaker.front.common.Routes
import com.neo.sk.breaker.front.components.model.RelativeModel
import com.neo.sk.breaker.front.utils.{Http, JsFunc, Shortcut}
import org.scalajs.dom.Event
import org.scalajs.dom.html.Input
import io.circe.syntax._

import scala.concurrent.ExecutionContext.Implicits.global
import io.circe.generic.auto._
import mhtml.Var

/**
  * Created by sky
  * Date on 2018/12/28
  * Time at 上午11:41
  */
object AlertModel{
  val mainYAlert=new AlertModel("top: 90px;width: 100%;background: none;text-align: center;box-shadow: none;",0)
  val mainNAlert=new AlertModel("top: 90px;width: 100%;background: none;text-align: center;box-shadow: none;",1)
}
class AlertModel(p:String,state:Int) extends RelativeModel(p){
  val text=Var("")
  override val modelDiv =
    <div class={state match {
      case 0=>
        "alert-model-b0"
      case 1=>
        "alert-model-b1"
      case 2=>
        "alert-model-b2"
      case _=>
        ""
    }}>
      <img src={state match {
        case 0=>
          Routes.getImgUrl("成功.png")
        case 1=>
          Routes.getImgUrl("创建失败.png")
        case 2=>
          Routes.getImgUrl("创建失败.png")
        case _=>
          Routes.getImgUrl("创建失败.png")
      }}></img>
      <span class={state match {
        case 0=>
          "alert-model-text0"
        case 1=>
          "alert-model-text0"
        case 2=>
          "alert-model-text1"
        case _=>
          "alert-model-text1"
      }}>{text}</span>
    </div>

  def show(s:String,t:Long,f:Boolean=true):Unit={
    text:=s
    showQRCode:=true
    if(f) Shortcut.scheduleOnce(()=>showQRCode:=false,t)
  }
}

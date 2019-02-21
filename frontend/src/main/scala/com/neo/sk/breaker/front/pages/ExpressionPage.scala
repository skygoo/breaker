package com.neo.sk.breaker.front.pages

import com.neo.sk.breaker.front.common.{Page, Routes}

import scala.xml.Elem
import com.neo.sk.breaker.shared.model.Constants.ExpressionMap
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.KeyboardEvent
/**
  * Created by sky
  * Date on 2019/2/21
  * Time at 上午10:09
  */
case class ExpressionPage(clickCallback:(Byte,Option[String])=>Unit) extends Page{
  var text=""
  override def render: Elem = {
    <div style="text-align: center;display: table-cell;vertical-align: middle;">
      {
        ExpressionMap.list.map{e=>
          <img src={Routes.getImgUrl("exp/"+e._2+".png")} class="expImg" onclick={()=>clickCallback(e._1,None)}></img>
        }
      }

      <div style="padding-top: 10%;">
        <input id="textInput" placeholder="输入内容,回车发送"  maxlength="20"
               style="background: #FFFFFF;border: 1px solid rgba(142,152,180,0.50);border-radius: 4px;height: 32px;padding-right:55px" oninput={e: Event =>
          dom.document.getElementById("textInput").asInstanceOf[Input].style.border = "1px solid rgba(142,152,180,0.50)"
          val elem = e.target.asInstanceOf[Input]
              text=elem.value} onkeypress={e:KeyboardEvent=> if(e.keyCode == 13){
              if(text!=""){
                clickCallback(0,Some(text))}
              dom.document.getElementById("textInput").asInstanceOf[Input].value=""
              text=""
              e.preventDefault()
            }}></input>
      </div>
    </div>
  }

  val a= <div class="button-all" style="width:94px;margin: auto;margin-top:30px;" onclick={()=>
    if(text!=""){
      clickCallback(0,Some(text))}
    dom.document.getElementById("textInput").asInstanceOf[Input].value=""
    text=""
  }>发送</div>
}

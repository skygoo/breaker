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
      <div style="margin-bottom: 10%;font-size: x-large;padding: 3px;">
        <span style="user-select:none;color: brown;">游戏规则</span>
        <div style="text-align: left;user-select:none;">
          <span class="gamespan">将离自身最近一行消除即可在对面生成一行；</span>
          <span class="gamespan">平板可以发射和反射小球；</span>
          <span class="gamespan">发射状态充满可以发射；</span>
          <span class="gamespan">小球撞击“+”填充发射状态；</span>
          <span class="gamespan">小球撞击“-”消失；</span>
          <span class="gamespan">小球最大撞击次数为15；将要消失时变为灰色</span>
        </div>
        <span style="user-select:none;color: brown;">胜利条件</span>
        <div style="text-align: left;user-select:none;">
          <span class="gamespan">当对手方无位置生成砖块即胜利；</span>
          <span class="gamespan">15分钟内撞击次数最多者即胜利；</span>
        </div>
      </div>
      {
        ExpressionMap.list.map{e=>
          <img src={Routes.getImgUrl("exp/"+e._2+".png")} class="expImg" onclick={()=>clickCallback(e._1,None)}></img>
        }
      }
      <div style="padding-top: 10%;">
        <input id="textInput" placeholder="输入内容,回车发送"  maxlength="20"
               style="user-select:none;background: #FFFFFF;border: 1px solid rgba(142,152,180,0.50);border-radius: 4px;height: 32px;padding-right:55px" oninput={e: Event =>
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

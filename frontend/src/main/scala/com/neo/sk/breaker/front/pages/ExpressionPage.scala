package com.neo.sk.breaker.front.pages

import com.neo.sk.breaker.front.common.{Page, Routes}

import scala.xml.Elem
import com.neo.sk.breaker.shared.model.Constants.ExpressionMap
/**
  * Created by sky
  * Date on 2019/2/21
  * Time at 上午10:09
  */
case class ExpressionPage(clickCallback:(Byte,Option[String])=>Unit) extends Page{

  override def render: Elem = {
    <div style="text-align: center;">
      {
        ExpressionMap.list.map{e=>
          <img src={Routes.getImgUrl("exp/"+e._2+".png")} class="expImg" onclick={()=>clickCallback(e._1,None)}></img>
        }
      }

      <input>

      </input>
    </div>
  }
}

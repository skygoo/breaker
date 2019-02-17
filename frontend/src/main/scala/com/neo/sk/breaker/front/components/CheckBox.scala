package com.neo.sk.breaker.front.components

import com.neo.sk.breaker.front.common.Routes
import mhtml.Var
import org.scalajs.dom.Event

/**
  * Created by sky
  * Date on 2018/12/18
  * Time at 上午11:20
  * 复选框
  */
case class CheckBox[A](isCheck: Boolean,info:A, checkCallBack: A => Unit, unCheckCallBack: A => Unit) {
  private val check = Var(isCheck)
  val box = check.map {
    case true =>
      <img src={Routes.getImgUrl("选中.png")} style="cursor: pointer;" onclick={() =>
        unCheckCallBack(info)
        check := false
      }></img>
    case false =>
      <div class="list-check-box" onclick={() =>
        checkCallBack(info)
        check := true}></div>
  }
}


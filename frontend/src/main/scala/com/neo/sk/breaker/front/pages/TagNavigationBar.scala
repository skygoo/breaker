package com.neo.sk.breaker.front.pages

import com.neo.sk.breaker.front.common.Routes
import mhtml.Var

/**
  * Created by sky
  */
object TagNavigationBar {

  var conList= List((0,"用户管理"),(1,"房间管理"))

  var showFlag = Var(0)

  def visibilityClass(id: Int) = showFlag.map { r =>
    if (r == id) "bar active" else "bar"
  }

  private val list =
    <div class ="sideList">
      {conList.map(t =>
      <div width="100%">
      <div class ={visibilityClass(t._1)} width="80%" display ="inline-block" onclick ={() => showFlag:=t._1}>
        <span>{t._2}</span>
      </div>
    </div>)}
    </div>

  def render =
    <div class ="leftBar">
      <div class ="top">
        <img src ={Routes.getImgUrl("统计.png")}></img>
        统计分析
      </div>
      <div class ="sideBar">
        {list}
      </div>
    </div>

}

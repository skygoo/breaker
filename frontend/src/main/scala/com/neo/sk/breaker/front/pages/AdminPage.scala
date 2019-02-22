package com.neo.sk.breaker.front.pages

import com.neo.sk.breaker.front.common.Page
import com.neo.sk.breaker.front.utils.Shortcut

import scala.xml.Elem

/**
  * Created by sky
  * Date on 2019/2/21
  * Time at 下午4:10
  */
object AdminPage extends Page{
  override def render: Elem = {
    <div style="height:100%">
      管理员界面
    </div>
  }
}

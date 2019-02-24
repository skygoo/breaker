package com.neo.sk.breaker.front.pages

import com.neo.sk.breaker.front.common.Page
import com.neo.sk.breaker.front.components.AlertModel

import scala.xml.Elem

/**
  * Created by sky
  * Date on 2019/2/24
  * Time at 下午3:06
  */
object AdminMainPage extends Page {
  val rightDiv=TagNavigationBar.showFlag.map{
    case _=> UserListPage.render
  }

  override def render: Elem =
    <div>
      <div style="position:relative;width: 100%;">
        {AlertModel.mainYAlert.QRCodeBox}
        {AlertModel.mainNAlert.QRCodeBox}
      </div>
      {TagNavigationBar.render}
      <div class="rightBody">
        {rightDiv}
      </div>
    </div>
}

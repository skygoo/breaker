package com.neo.sk.breaker.front.pages

import com.neo.sk.breaker.front.common.{Page, Routes}
import com.neo.sk.breaker.front.utils.{Http, Shortcut}
import com.neo.sk.breaker.shared.protocol.UserProtocol.UserLoginReq
import com.neo.sk.breaker.shared.ptcl.SuccessRsp
import mhtml.{Var, emptyHTML}
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.html.Input
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Elem

/**
  * Created by sky
  * Date on 2019/2/21
  * Time at 下午4:10
  */
object AdminLoginPage extends Page {

  def login(): Unit = {
    val userId = dom.document.getElementById("loginId").asInstanceOf[Input].value
    val psw = dom.document.getElementById("loginPassword").asInstanceOf[Input].value
    if (userId != "" && psw != "") {
      val url = Routes.Admin.login
      val data = UserLoginReq(userId, psw).asJson.noSpaces
      Http.postJsonAndParse[SuccessRsp](url, data).map {
        rsp =>
          if (rsp.errCode == 0) {
            HeaderBar.adminIdVar := Some(userId)
            Shortcut.redirect("#/admin/userList")
          } else {
            MainPage.createConfirm(rsp.msg)
          }
      }
    } else {
      MainPage.createConfirm("输入不可为空")
    }
  }


  override def render: Elem =
    <div style="width:100%;height:100%;">
      <div id="filter-main">
        <div style="padding: 20px;">
          <div class="form-content" style="width:80%;margin: auto;">
            <input class="form-control input_class" id="loginId" placeholder="userId"></input>
            <input type="password" class="form-control input_class" id="loginPassword" placeholder="password"></input>
          </div>
          <div class="form-submit input_class">
            <div class="button-all" style="width:94px;margin: auto;" onclick={() => login()}>登录</div>
          </div>
          <div class="form-information">
          </div>
        </div>
      </div>
    </div>


}

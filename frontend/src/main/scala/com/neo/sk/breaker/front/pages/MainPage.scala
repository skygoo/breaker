package com.neo.sk.breaker.front.pages

import com.neo.sk.breaker.front.common.{Page, PageSwitcher, Routes}
import com.neo.sk.breaker.front.components.ConfirmModel
import com.neo.sk.breaker.front.utils.Http
import com.neo.sk.breaker.shared.model.Constants
import com.neo.sk.breaker.shared.protocol.UserProtocol.GetUserInfoRsp
import mhtml.{Cancelable, Rx, Var, mount}
import org.scalajs.dom
import mhtml.emptyHTML

import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Elem

/**
  * Created by hongruying on 2018/4/8
  */
object MainPage extends PageSwitcher {

  override def switchPageByHash(): Unit = {
    val tokens = {
      val t = getCurrentHash.split("/").toList
      if (t.nonEmpty) {
        t.tail
      } else Nil
    }

    println(tokens)
    switchToPage(tokens)
  }

  private val headerPage = currentPageHash.map {
    case "admin" :: l =>
      HeaderBar.render
    case _ => emptyHTML
  }

  private val currentPage: Rx[Elem] = currentPageHash.map {
    case Nil => LoginPage.render
    case "play" :: roomType :: Nil => {
      new PlayPage(roomType.toByte, LoginPage.playerInfo).render
    }
    case "admin" :: l =>
      <div class="conBody">
        <div style="text-align: center;">
          {l match {
          case "login":: Nil =>
            AdminLoginPage.render
          case "userList" :: Nil =>
            AdminMainPage.render
          case _ =>
            <div>Error Page</div>
        }}
        </div>
      </div>

    case _ => <div>Error Page</div>
  }


  //询问框
  val confirmVar = Var(<div></div>)

  def createConfirm(s: String, commitCallBack: => Unit = {}, f: Boolean = false) = {
    val alertModel = new ConfirmModel(s, commitCallBack, f)
    confirmVar := <div>
      {alertModel.QRCodeBox}
    </div>
    alertModel.showQRCode := true
  }

  def show(): Cancelable = {
    switchPageByHash()
    val page =
      <div style="width:100%;height:100%;">
        {confirmVar}{headerPage}{currentPage}
      </div>
    mount(dom.document.body, page)
  }

  def getUserInfo(): Unit = {
    Http.getAndParse[GetUserInfoRsp](Routes.User.getUserInfo).map { rsp =>
      if (rsp.errCode == 0 && rsp.userType.getOrElse(0) == Constants.userType) {
        LoginPage.userIdVar := Some(rsp.userId.getOrElse(""))
      } else if (rsp.errCode == 0 && rsp.userType.getOrElse(0) == Constants.adminType) {
        HeaderBar.adminIdVar := Some(rsp.userId.getOrElse(""))
      } else {
        LoginPage.userIdVar := None
      }
    }
  }
}

package com.neo.sk.breaker.front.pages

import com.neo.sk.breaker.front.common.{Page, PageSwitcher}
import com.neo.sk.breaker.front.components.ConfirmModel
import mhtml.{Cancelable, Rx, Var, mount}
import org.scalajs.dom

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


  private val currentPage: Rx[Elem] = currentPageHash.map {
    case Nil => LoginPage.render
    case "play":: roomType :: Nil => {
      new PlayPage(roomType.toByte,LoginPage.playerInfo).render
    }
    case "admin"::Nil=> AdminPage.render

    case _ => <div>Error Page</div>
  }


  //询问框
  val confirmVar = Var(<div></div>)

  def createConfirm(s: String, commitCallBack: => Unit = {}, f:Boolean = false) = {
    val alertModel=new ConfirmModel(s,commitCallBack,f)
    confirmVar:= <div>{alertModel.QRCodeBox}</div>
    alertModel.showQRCode:=true
  }
  def show(): Cancelable = {
    switchPageByHash()
    val page =
      <div style="width:100%;height:100%;">
        {confirmVar}
        {currentPage}
      </div>
    mount(dom.document.body, page)
  }

}

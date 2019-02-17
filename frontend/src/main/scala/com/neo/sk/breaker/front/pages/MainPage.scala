package com.neo.sk.breaker.front.pages

import com.neo.sk.breaker.front.common.{Page, PageSwitcher}
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
    case "play" :: Nil => {
      println("play")
      new PlayPage(LoginPage.playerInfo).render
    }
    case _ => <div>Error Page</div>
  }


  def show(): Cancelable = {
    switchPageByHash()
    val page =
      <div style="width:100%;height:100%;">
        {currentPage}
      </div>
    mount(dom.document.body, page)
  }

}

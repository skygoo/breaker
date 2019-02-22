package com.neo.sk.breaker.front

import com.neo.sk.breaker.front.pages.{LoginPage, MainPage}

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

/**
  * User: Taoz
  * Date: 6/3/2017
  * Time: 1:03 PM
  */
@JSExportTopLevel("front.Main")
object Main {


  @JSExport
  def run(): Unit = {
    LoginPage.getUserInfo()
    MainPage.show()
  }



}

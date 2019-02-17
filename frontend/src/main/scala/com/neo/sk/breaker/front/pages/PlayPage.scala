package com.neo.sk.breaker.front.pages

import com.neo.sk.breaker.front.client.control.GamePlayHolderImpl
import com.neo.sk.breaker.front.common.Page
import com.neo.sk.breaker.front.utils.Shortcut
import com.neo.sk.breaker.shared.protocol.UserProtocol.UserInfo

import scala.xml.Elem

/**
  * Created by sky
  * Date on 2019/2/17
  * Time at 下午8:34
  */
class PlayPage(playerInfo:UserInfo) extends Page{
  private val canvas = <canvas id ="GameView" tabindex="1"></canvas>

  def init() = {
    val gameHolder = new GamePlayHolderImpl("GameView", playerInfo)
    gameHolder.start
  }

  override def render: Elem ={
    Shortcut.scheduleOnce(() =>init(),0)
    <div>
      {canvas}
    </div>
  }
}

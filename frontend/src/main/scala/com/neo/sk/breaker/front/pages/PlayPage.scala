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
class PlayPage(playerInfo: UserInfo) extends Page {
  private val canvas = <canvas id="GameView" tabindex="1"></canvas>
  var gameHolder: Option[GamePlayHolderImpl] = None

  def init() = {
    gameHolder = Some(GamePlayHolderImpl("GameView", playerInfo))
    gameHolder.foreach(_.start)
  }

  def expressionCallback(et:Byte,s:Option[String])={
    gameHolder.foreach(_.sendExpression(et,s))
  }

  override def render: Elem = {
    Shortcut.scheduleOnce(() => init(), 0)
    <div style="height:100%">
      {canvas}<div style="float:right;width:20%;height:100%;display: table;">
      {ExpressionPage(expressionCallback).render}
    </div>
    </div>
  }
}

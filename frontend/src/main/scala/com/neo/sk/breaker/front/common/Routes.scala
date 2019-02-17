package com.neo.sk.breaker.front.common

import com.neo.sk.breaker.shared.protocol.UserProtocol.UserInfo
import org.scalajs.dom

/**
  * User: Taoz
  * Date: 2/24/2017
  * Time: 10:59 AM
  */
object Routes {

  val base = "/breaker"

  val getRoomListRoute = base + "/getRoomIdList"

  def getImgUrl(imgName:String) = base + s"/static/img/${imgName}"

  def wsJoinGameUrl(name: String) = {
    base + s"/game/join?name=$name"
  }

  def wsJoinGameUrl(name: String, userId: String, playerId: String): String = {
    base + s"/game/join?name=$name&userId=$userId&playerId=$playerId"
  }

  def getJoinGameWebSocketUri(playerInfo: UserInfo): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    playerInfo.playerId match {
      case Some(p) =>
        s"$wsProtocol://${dom.document.location.host}${Routes.wsJoinGameUrl(playerInfo.nickName, playerInfo.userName.getOrElse(""), playerInfo.playerId.getOrElse(""))}"
      case None =>
        s"$wsProtocol://${dom.document.location.host}${Routes.wsJoinGameUrl(playerInfo.nickName)}"
    }
  }


}

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

  def wsJoinGameUrl(name: String, roomIdOpt: Option[Long]) = {
    base + s"/game/join?name=${name}" +
      (roomIdOpt match {
        case Some(roomId) => s"&roomId=$roomId"
        case None => ""
      })
  }

  def wsJoinGameUrl(name: String, userId: String, userName: String, roomIdOpt: Option[Long]): String = {
    base + s"/game/userJoin?name=$name&userId=$userId&userName=$userName" +
      (roomIdOpt match {
        case Some(roomId) =>
          s"&roomId=$roomId"
        case None =>
          ""
      })
  }

  def getJoinGameWebSocketUri(playerInfo: UserInfo, roomIdOpt: Option[Long]): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    playerInfo.playerId match {
      case Some(p) =>
        s"$wsProtocol://${dom.document.location.host}${Routes.wsJoinGameUrl(playerInfo.nickName, playerInfo.userName.getOrElse(""), playerInfo.playerId.getOrElse(""), roomIdOpt)}"
      case None =>
        s"$wsProtocol://${dom.document.location.host}${Routes.wsJoinGameUrl(playerInfo.nickName, roomIdOpt)}"
    }
  }


}

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

  def getImgUrl(imgName: String) = base + s"/static/img/$imgName"

  def wsJoinGameUrl(name: String, userId: Option[String], playerId: Option[String]): String = {
    base + s"/game/join?name=$name" + {
      userId match {
        case Some(v) =>
          s"&userId=$v"
        case None => ""
      }
    } + {
      playerId match {
        case Some(v) =>
          s"&playerId=$v"
        case None => ""
      }
    }
  }

  def getJoinGameWebSocketUri(playerInfo: UserInfo): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}${Routes.wsJoinGameUrl(playerInfo.nickName, playerInfo.userName, playerInfo.playerId)}"
  }

  object User {
    val userBase = base + "/user"

    val sign = userBase + "/sign"

    val login = userBase + "/login"

    val logout = userBase + "/logout"

    val getUserInfo = userBase + "/getUserInfo"

    val getUserList = userBase +"/getUserList"

    val addState4User = userBase +"/addState4User"
  }

  object Admin {
    val userBase = base + "/admin"

    val login = userBase + "/login"

    val logout = userBase + "/logout"

    val getUserInfo = userBase + "/getUserInfo"

  }


}

package com.neo.sk.breaker.http

import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.BasicDirectives
import com.neo.sk.breaker.common.AppSettings
import com.neo.sk.utils.SessionSupport
import org.slf4j.LoggerFactory


/**
  * Created by hongruying on 2018/3/11
  */

object SessionBase {

  private val SessionTypeKey = "STKey"

  object AccountSessionKeys {
    val sessionType = "user_session"
    val name = "name"
    val timestamp = "user_timestamp"
  }


  val log = LoggerFactory.getLogger(this.getClass)

  case class AccountSession(
                             userId: String,
                             time: Long
                           ) {
    def toSessionMap = Map(
      SessionTypeKey -> AccountSessionKeys.sessionType,
      AccountSessionKeys.name -> userId,
      AccountSessionKeys.timestamp -> time.toString
    )
  }

  object SessionKeys {
    val accountType = "yilia_account_type"
    val accountId = "yilia_accountId"
    val timestamp = "yilia_timestamp"
    val nickname = "yilia_nickname"
    val headImg = "yilia_headImg"
  }

  case class SessionCombine(
                             userType: Int,
                             id: String,
                             timestamp: Long,
                           ) {
    def toSessionMap = Map(
      SessionKeys.accountType -> userType.toString,
      SessionKeys.accountId -> id,
      SessionKeys.timestamp -> timestamp.toString
    )
  }


}

trait SessionBase extends SessionSupport {

  import SessionBase._

  override val sessionEncoder = SessionSupport.PlaySessionEncoder
  override val sessionConfig = AppSettings.sessionConfig
  protected val sessionTimeout = 24 * 60 * 60 * 1000
  private val log = LoggerFactory.getLogger(this.getClass)

  implicit class SessionTransformer(sessionMap: Map[String, String]) {
    def toAccountSession: Option[AccountSession] = {
      //      log.debug(s"toAdminSession: change map to session, ${sessionMap.mkString(",")}")
      try {
        if (sessionMap.get(SessionTypeKey).exists(_.equals(AccountSessionKeys.sessionType))) {
          if (sessionMap(AccountSessionKeys.timestamp).toLong - System.currentTimeMillis() > sessionTimeout) {
            None
          } else {
            Some(AccountSession(
              //              sessionMap(AccountSessionKeys.userType).toInt,
              //              sessionMap(AccountSessionKeys.accountId).toLong,
              sessionMap(AccountSessionKeys.name),
              //              sessionMap(AccountSessionKeys.nameCn),
              sessionMap(AccountSessionKeys.timestamp).toLong
            ))
          }
        } else {
          log.debug("no session type in the session")
          None
        }
      } catch {
        case e: Exception =>
          e.printStackTrace()
          log.warn(s"toAdminSession: ${e.getMessage}")
          None
      }
    }

    def toUserSession: Option[SessionCombine] = {
      log.debug(s"toUserSession: change map to session, ${sessionMap.mkString(",")}")
      try {
        if (sessionMap.get(SessionKeys.accountType).nonEmpty) {
          Some(SessionCombine(
            sessionMap(SessionKeys.accountType).toInt,
            sessionMap(SessionKeys.accountId),
            sessionMap(SessionKeys.timestamp).toLong
          ))
        }
        else {
          log.debug("no session type in the session")
          None
        }
      } catch {
        case e: Exception =>
          e.printStackTrace()
          log.warn(s"toUserSession: ${e.getMessage}")
          None
      }
    }
  }

  protected val optionalAccountSession: Directive1[Option[AccountSession]] = optionalSession.flatMap {
    case Right(sessionMap) => BasicDirectives.provide(sessionMap.toAccountSession)
    case Left(error) =>
      log.debug(error)
      BasicDirectives.provide(None)
  }
  protected val optionalUserSession: Directive1[Option[SessionCombine]] = optionalSession.flatMap {
    case Right(sessionMap) => BasicDirectives.provide(sessionMap.toUserSession)
    case Left(error) =>
      log.debug(error)
      BasicDirectives.provide(None)
  }
}

package com.neo.sk.breaker.http

import akka.http.scaladsl.server.Directives.pathPrefix
import java.util.concurrent.atomic.AtomicLong

import akka.actor.{ActorSystem, Scheduler}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.neo.sk.breaker.common.AppSettings
import akka.actor.typed.scaladsl.AskPattern._

import scala.concurrent.{ExecutionContextExecutor, Future}
import com.neo.sk.breaker.Boot.{executor, scheduler, timeout}
import com.neo.sk.breaker.core.UserManager
import com.neo.sk.breaker.http.SessionBase.{SessionCombine, SessionKeys}
import com.neo.sk.breaker.models.DAO.{SignUserInfo, UserInfoDAO}
import com.neo.sk.breaker.shared.ptcl.{ErrorRsp, SuccessRsp}
import io.circe._
import org.slf4j.LoggerFactory
import com.neo.sk.breaker.protocol.CommonErrorCode.parseJsonError
import com.neo.sk.breaker.shared.model.Constants
import com.neo.sk.breaker.shared.protocol.UserProtocol.{GetUserInfoRsp, UserLoginReq}

/**
  * Created by sky
  * Date on 2019/2/24
  * Time at 上午10:49
  */
trait AdminService extends ServiceUtils {
  import com.neo.sk.utils.CirceSupport._
  import io.circe.generic.auto._

  private val log = LoggerFactory.getLogger(this.getClass)

  //管理员登录
  private val adminLogin=(path("login") & post) {
    entity(as[Either[Error, UserLoginReq]]) {
      case Left(error) =>
        log.warn(s"some error: $error")
        complete(parseJsonError)
      case Right(req) =>
        val adminAccount = AppSettings.adminAccount
        if(adminAccount.exists(_._2 == req.userId)){
          val admin = adminAccount.find(_._2 == req.userId).get
          if(admin._3 == req.password){
            val session = SessionCombine(Constants.adminType,req.userId,System.currentTimeMillis()).toSessionMap
            addSession(session){
              log.info(s"admin ${req.userId} login success")
              complete(SuccessRsp())
            }
          }
          else complete(ErrorRsp(10002,s"密码错误"))
        }
        else complete(ErrorRsp(10003,"用户名不存在"))
    }
  }

  //管理员登出
  private val logout = (path("logout") & get){
    authUser { u =>
      val session = Set(SessionKeys.accountType,SessionKeys.accountId,SessionKeys.timestamp)
      removeSession(session){ctx =>
        log.info(s"admin-----${u.id}----logout")
        ctx.complete(SuccessRsp())
      }
    }
  }

  val adminRoutes = pathPrefix("admin") {
    adminLogin ~ logout
  }
}

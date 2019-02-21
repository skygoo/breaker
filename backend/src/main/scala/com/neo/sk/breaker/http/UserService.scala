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
import com.neo.sk.breaker.common.{AppSettings, Constants}
import akka.actor.typed.scaladsl.AskPattern._

import scala.concurrent.{ExecutionContextExecutor, Future}
import com.neo.sk.breaker.Boot.{executor, scheduler, timeout}
import com.neo.sk.breaker.core.UserManager
import com.neo.sk.breaker.models.DAO.{SignUserInfo, UserInfoDAO}
import com.neo.sk.breaker.shared.ptcl.{ErrorRsp, SuccessRsp}
import io.circe._
import org.slf4j.LoggerFactory
import com.neo.sk.breaker.protocol.CommonErrorCode.parseJsonError
import com.neo.sk.breaker.shared.protocol.UserProtocol.{UserLoginReq, UserSignReq}
/**
  * Created by sky
  * Date on 2019/2/21
  * Time at 下午3:04
  */
trait UserService extends ServiceUtils {
  import com.neo.sk.utils.CirceSupport._
  import io.circe.generic.auto._

  private val log = LoggerFactory.getLogger(this.getClass)

  private val login = (path("login") & post) {
    entity(as[Either[Error, UserLoginReq]]) {
      case Right(req) =>
        dealFutureResult(
          UserInfoDAO.getLoginInfo(req.userId).map{
            case Some(u)=>
              if(u.password!=req.password){
                complete(ErrorRsp(10002,s"密码错误"))
              }else{
                complete(SuccessRsp())
              }
            case None=>
              complete(SuccessRsp())
          }.recover{
            case e:Exception =>
              log.debug(s"获取用户信息失败，recover error:$e")
              complete(ErrorRsp(10001,s"获取用户信息失败，recover error:$e"))
          }
        )
      case Left(e) =>
        log.error(s"json parse error: $e")
        complete(parseJsonError)
    }
  }

  private val sign = (path("sign") & post){
    entity(as[Either[Error, UserSignReq]]) {
      case Right(req) =>
        dealFutureResult(
          for{
            u <- UserInfoDAO.getUserInfoById(req.userId)
            m <- UserInfoDAO.getUserInfoById(req.userId)
          }yield{
            if(u.nonEmpty){
              complete(ErrorRsp(10011,s"用户名已存在"))
            }else if(m.nonEmpty){
              complete(ErrorRsp(10012,s"邮箱已注册"))
            }else{
              dealFutureResult(
                UserInfoDAO.insertInfo(SignUserInfo(req.userId,req.mail,req.password,System.currentTimeMillis(),Constants.stateAllow)).map(t=>
                  if(t>0){
                    complete(SuccessRsp())
                  }else{
                    complete(ErrorRsp(10012,s"注册失败"))
                  }
                )
              )
            }
          }
        )
      case Left(e) =>
        log.error(s"json parse error: $e")
        complete(parseJsonError)
    }
  }

  val userRoutes = pathPrefix("user") {
    login ~ sign
  }
}

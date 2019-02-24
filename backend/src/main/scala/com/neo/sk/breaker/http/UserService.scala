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
import com.neo.sk.breaker.protocol.CommonErrorCode.{parseJsonError, userAuthError}
import com.neo.sk.breaker.shared.model.Constants
import com.neo.sk.breaker.shared.protocol.UserProtocol._
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
                u.state match {
                  case Constants.stateAllow =>
                    val session = SessionCombine(Constants.userType,u.userId,System.currentTimeMillis()).toSessionMap
                    addSession(session){
                      log.info(s"user ${req.userId} login success")
                      complete(SuccessRsp())
                    }
                  case _ =>
                    complete(ErrorRsp(10004,"账号被禁用"))
                }
              }
            case None=>
              complete(ErrorRsp(10003,"用户名不存在"))
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
                    val session = SessionCombine(Constants.userType,req.userId,System.currentTimeMillis()).toSessionMap
                    addSession(session){
                      log.info(s"user ${req.userId} sign and login success")
                      complete(SuccessRsp())
                    }
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

  private val logout = (path("logout") & get){
    authUser { u =>
      val session = Set(SessionKeys.accountType,SessionKeys.accountId,SessionKeys.timestamp)
      removeSession(session){ctx =>
        log.info(s"user-----${u.id}----logout")
        ctx.complete(SuccessRsp())
      }
    }
  }

  private val getUserInfo = (path("getUserInfo") & get){
    authUser{u=>
      complete(GetUserInfoRsp(Some(u.userType),Some(u.id)))
    }
  }

  private val getUserList = (path("getUserList") & post){
    authUser{u=>
      if(u.userType==Constants.adminType){
        entity(as[Either[Error, GetUserListReq]]) {
          case Right(req) =>
            dealFutureResult(
              for {
                list <- UserInfoDAO.getUserList(req)
                num <- if (req.page == 1) UserInfoDAO.getListNumByState(req.state).map(Some(_)) else Future.successful(None)
              } yield {
                val data = list.map(u => ShowUserInfo(u._1, u._2, u._3,u._4))
                complete(GetUserListRsp(num, Some(data.toList)))
              }
            )
          case Left(e) =>
            log.error(s"json parse error: $e")
            complete(parseJsonError)
        }
      }else{
        complete(userAuthError)
      }
    }
  }

  private val addState4User = (path("addState4User") & post){
    authUser{u=>
      if(u.userType==Constants.adminType){
        entity(as[Either[Error, AddState4UserReq]]) {
          case Right(req) =>
            dealFutureResult(
              UserInfoDAO.updateListInfo(req.user.toSet,req.state).map{r=>
                if(r>0){
                  complete(SuccessRsp())
                }else{
                  complete(ErrorRsp(10012,s"更新失败"))
                }
              }
            )
          case Left(e) =>
            log.error(s"json parse error: $e")
            complete(parseJsonError)
        }
      }else{
        complete(userAuthError)
      }
    }
  }

  val userRoutes = pathPrefix("user") {
    login ~ logout ~ sign ~ getUserInfo ~ getUserList ~ addState4User
  }
}

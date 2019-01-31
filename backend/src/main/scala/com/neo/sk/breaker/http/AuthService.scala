package com.neo.sk.breaker.http

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
import com.neo.sk.breaker.Boot.{esheepSyncClient, executor, scheduler, timeout}
import com.neo.sk.breaker.core.EsheepSyncClient
import com.neo.sk.breaker.protocol.EsheepProtocol
import com.neo.sk.breaker.shared.ptcl.ErrorRsp
import org.slf4j.LoggerFactory

/**
  * Created by hongruying on 2018/10/18
  */
trait AuthService extends ServiceUtils{

  import com.neo.sk.utils.CirceSupport._
  import io.circe.generic.auto._

  private val log = LoggerFactory.getLogger(this.getClass)

  private def AuthUserErrorRsp(msg: String) = ErrorRsp(10001001, msg)

  val uid = new AtomicLong(1L)

  protected def authPlatUser(accessCode:String)(f: EsheepProtocol.PlayerInfo => server.Route):server.Route = {
    if(AppSettings.esheepAuthToken) {
      val verifyAccessCodeFutureRst: Future[EsheepProtocol.VerifyAccessCodeRsp] = esheepSyncClient ? (e => EsheepSyncClient.VerifyAccessCode(accessCode, e))
      dealFutureResult{
        verifyAccessCodeFutureRst.map{ rsp =>
          if(rsp.errCode == 0 && rsp.data.nonEmpty){
            f(rsp.data.get)
          } else{
            complete(AuthUserErrorRsp(rsp.msg))
          }
        }.recover{
          case e:Exception =>
            log.warn(s"verifyAccess code failed, code=${accessCode}, error:${e.getMessage}")
            complete(AuthUserErrorRsp(e.getMessage))
        }
      }
    } else {
      val id = uid.getAndIncrement()
      f(EsheepProtocol.PlayerInfo(s"test_${id}",s"test_${id}"))
    }
  }

}

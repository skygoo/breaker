package com.neo.sk.breaker.http

import java.net.URLEncoder

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive, RequestContext, Route}
import com.neo.sk.breaker.common.{AppSettings, Constants}
import com.neo.sk.breaker.protocol.CommonErrorCode._
import com.neo.sk.breaker.shared.ptcl.ErrorRsp
import com.neo.sk.utils.CirceSupport
import com.neo.sk.utils.SecureUtil._
import com.neo.sk.breaker.http.SessionBase.SessionCombine

import com.neo.sk.breaker.protocol.CommonErrorCode
import io.circe.{Decoder, Error}
import io.circe.parser.decode
import org.slf4j.LoggerFactory
import com.neo.sk.breaker.Boot.executor
import com.neo.sk.breaker.common.Constants

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * User: Taoz
  * Date: 11/18/2016
  * Time: 7:57 PM
  */

object ServiceUtils{
  private val log = LoggerFactory.getLogger(this.getClass)
  private val authCheck = AppSettings.authCheck
}

trait ServiceUtils extends CirceSupport with SessionBase{

  import ServiceUtils._
  import io.circe.generic.auto._



  def htmlResponse(html: String): HttpResponse = {
    HttpResponse(entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, html))
  }

  def jsonResponse(json: String): HttpResponse = {
    HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, json))
  }

  def dealFutureResult(future: => Future[server.Route]): Route = {
    onComplete(future) {
      case Success(rst) => rst
      case Failure(e) =>
        e.printStackTrace()
        log.error("internal error: {}", e.getMessage)
        complete(ErrorRsp(1000, "internal error."))
    }
  }

  def loggingAction: Directive[Tuple1[RequestContext]] = extractRequestContext.map { ctx =>
    log.info(s"Access uri: ${ctx.request.uri} from ip ${ctx.request.uri.authority.host.address}.")
    ctx
  }

  def dealPostReq[A](f: A => Future[server.Route])(implicit decoder: Decoder[A]): server.Route = {
    entity(as[Either[Error, PostEnvelope]]) {
      case Right(envelope) =>
          dealFutureResult {
            decode[A](envelope.data) match {
              case Right(req) =>
                f(req)

              case Left(e) =>
                log.error(s"json parse detail type,data=${envelope.data} error: $e")
                Future.successful(complete(parseJsonError))
            }
          }

      case Left(e) =>
        log.error(s"json parse PostEnvelope error: ${e}")
        complete(parseJsonError)
    }
  }

  def dealGetReq(f: => Future[server.Route]): server.Route = {
    entity(as[Either[Error, PostEnvelope]]) {
      case Right(envelope) =>
        dealFutureResult(f)
      case Left(e) =>
        log.error(s"json parse PostEnvelope error: $e")
        complete(parseJsonError)
    }
  }

  def authUser(f: SessionCombine => server.Route) = loggingAction { ctx =>
    optionalUserSession {
      case Some(u) =>
        f(u)
      case None =>
        complete(CommonErrorCode.noSessionError())
    }
  }







}

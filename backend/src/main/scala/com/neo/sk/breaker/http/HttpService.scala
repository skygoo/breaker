package com.neo.sk.breaker.http

import java.net.URLEncoder

import akka.actor.{ActorSystem, Scheduler}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.neo.sk.breaker.common.AppSettings
import akka.actor.typed.scaladsl.AskPattern._

import scala.concurrent.{ExecutionContextExecutor, Future}
import com.neo.sk.breaker.Boot.{executor, scheduler, timeout}
import com.neo.sk.breaker.core.UserManager
import com.neo.sk.breaker.shared.ptcl.ErrorRsp
import com.neo.sk.breaker.Boot.{executor, roomManager, scheduler, timeout, userManager}
import com.neo.sk.breaker.shared.protocol.UserProtocol.UserInfo

import scala.util.Random

/**
  * Created by sky
  * Date on 2019/2/2
  * Time at 下午8:59
  */
trait HttpService
  extends ResourceService
    with ServiceUtils{

  import akka.actor.typed.scaladsl.AskPattern._
  import com.neo.sk.utils.CirceSupport._
  import io.circe.generic.auto._
  import io.circe._

  implicit val system: ActorSystem

  implicit val executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val timeout: Timeout

  implicit val scheduler: Scheduler
  import akka.actor.typed.scaladsl.adapter._




  lazy val routes: Route = pathPrefix("breaker"){
    resourceRoutes ~
      (pathPrefix("game") & get){
        pathEndOrSingleSlash{
          getFromResource("html/index.html")
        }~path("join"){
          parameter(
            'name,
            'userId.as[String].?,
            'playerId.as[String].?
          ){ (name,userId,playerId) =>
            val flowFuture:Future[Flow[Message,Message,Any]] = userManager ? (UserManager.GetWebSocketFlow(_,UserInfo(name,userId,playerId)))
            dealFutureResult(
              flowFuture.map(t => handleWebSocketMessages(t))
            )
          }
        }
      }
  }




}

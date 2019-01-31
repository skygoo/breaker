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
import com.neo.sk.breaker.Boot.roomManager

import scala.concurrent.{ExecutionContextExecutor, Future}
import com.neo.sk.breaker.Boot.{executor, scheduler, timeout, userManager}
import com.neo.sk.breaker.shared.ptcl.ErrorRsp

import scala.util.Random

/**
  *
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
      (pathPrefix("play") & get){
        pathEndOrSingleSlash{
          getFromResource("html/index.html")
        }
      }
  }




}

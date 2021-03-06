package com.neo.sk.breaker.common

import java.util.concurrent.TimeUnit

import com.neo.sk.breaker.core.game.BreakGameConfigServerImpl
import com.neo.sk.utils.SessionSupport.SessionConfig
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer
/**
  * Created by hongruying on 2018/3/11
  */
object AppSettings {

  private implicit class RichConfig(config: Config) {
    val noneValue = "none"

    def getOptionalString(path: String): Option[String] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getString(path))

    def getOptionalLong(path: String): Option[Long] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getLong(path))

    def getOptionalDurationSeconds(path: String): Option[Long] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getDuration(path, TimeUnit.SECONDS))
  }


  val log = LoggerFactory.getLogger(this.getClass)
  val config = ConfigFactory.parseResources("product.conf").withFallback(ConfigFactory.load())
  val breakerGameConfig = BreakGameConfigServerImpl(ConfigFactory.parseResources("breakerGame.conf")).gameConfig



  val appConfig = config.getConfig("app")

  val personLimit = appConfig.getInt("breakerGameRoomManager.personLimit")
  val supportLiveLimit = appConfig.getBoolean("breakerGameRoomManager.supportLiveLimit")
  val botLimit = appConfig.getInt("breakerGameRoomManager.botLimit")
  val botSupport = appConfig.getBoolean("breakerGameRoomManager.botSupport")

  val nameList = appConfig.getStringList("botManager.nameList")
  val needSpecialName = appConfig.getBoolean("botManager.needSpecialName")

  val httpInterface = appConfig.getString("http.interface")
  val httpPort = appConfig.getInt("http.port")


  val authCheck = appConfig.getBoolean("authCheck")
  val ramblerAuthCheck = appConfig.getBoolean("ramblerAuthCheck")

  val gameDataDirectoryPath = appConfig.getString("gameDataDirectoryPath")
  val gameRecordIsWork = appConfig.getBoolean("gameRecordIsWork")
  val gameRecordTime = appConfig.getInt("gameRecordTime")


  val slickConfig = config.getConfig("slick.db")
  val slickUrl = slickConfig.getString("url")
  val slickUser = slickConfig.getString("user")
  val slickPassword = slickConfig.getString("password")
  val slickMaximumPoolSize = slickConfig.getInt("maximumPoolSize")
  val slickConnectTimeout = slickConfig.getInt("connectTimeout")
  val slickIdleTimeout = slickConfig.getInt("idleTimeout")
  val slickMaxLifetime = slickConfig.getInt("maxLifetime")



  val sessionConfig = {
    val sConf = config.getConfig("session")
    SessionConfig(
      cookieName = sConf.getString("cookie.name"),
      serverSecret = sConf.getString("serverSecret"),
      domain = sConf.getOptionalString("cookie.domain"),
      path = sConf.getOptionalString("cookie.path"),
      secure = sConf.getBoolean("cookie.secure"),
      httpOnly = sConf.getBoolean("cookie.httpOnly"),
      maxAge = sConf.getOptionalDurationSeconds("cookie.maxAge"),
      sessionEncryptData = sConf.getBoolean("encryptData")
    )
  }


  val adminAccount = {
    import collection.JavaConverters._
    val list = appConfig.getStringList("adminAccount").asScala
    val admins = new ListBuffer[(Long,String,String)]
    for(i <- list.indices){
      val (account,pwd) = (list(i).split(":")(0),list(i).split(":")(1))
      admins.append((i+1,account,pwd))
    }
    admins.toList
  }




}

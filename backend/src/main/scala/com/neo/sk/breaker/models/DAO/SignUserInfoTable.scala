package com.neo.sk.breaker.models.DAO

import scala.concurrent.Future
import com.neo.sk.breaker.Boot.executor
/**
  * copy from dry on 2018/10/24.
  *
  * edit by sky
  **/

case class SignUserInfo(userId:String,mail:String,password:String,createTime:Long,state:Int)

trait SignUserInfoTable {

  import com.neo.sk.utils.H2DBUtil.driver.api._

  class UserInfoTable(tag: Tag) extends Table[SignUserInfo](tag, "USER_INFO") {
    val userId =column[String]("USER_ID", O.PrimaryKey)
    val mail = column[String]("MAIL")
    val password = column[String]("PASSWORD")
    val createTime = column[Long]("CREATE_TIME")
    val state = column[Int]("STATE")

    def * = (userId,mail,password,createTime,state) <> (SignUserInfo.tupled, SignUserInfo.unapply)
  }

  protected val userInfoTableQuery = TableQuery[UserInfoTable]
}

object UserInfoDAO extends SignUserInfoTable {

  import com.neo.sk.utils.H2DBUtil.driver.api._
  import com.neo.sk.utils.H2DBUtil.db


  def create(): Future[Unit] = {
    db.run(userInfoTableQuery.schema.create)
  }

  def getAllUserInfo: Future[List[SignUserInfo]] = {
    db.run (userInfoTableQuery.to[List].result)
  }

  def insertInfo(mpInfo:SignUserInfo) = {
    db.run(userInfoTableQuery += mpInfo)
  }

  def getUserInfoById(userId: String) = {
    db.run(userInfoTableQuery.filter(m => m.userId===userId).result.headOption)
  }

  def getUserInfobyMail(mail: String) = {
    db.run(userInfoTableQuery.filter(m => m.mail===mail).result.headOption)
  }

  def getLoginInfo(i:String)={
    db.run(userInfoTableQuery.filter(m=>m.userId===i).result.headOption)
  }

  def updateUserInfo(userId:String, state:Int) = {
    db.run(userInfoTableQuery.filter(m => m.userId===userId).map(m => m.state).update(state))
  }

  def main(args: Array[String]): Unit = {
    create()
    Thread.sleep(2000)
  }
}
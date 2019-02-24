package com.neo.sk.breaker.models.DAO

import scala.concurrent.Future
import com.neo.sk.breaker.Boot.executor
import com.neo.sk.breaker.shared.protocol.UserProtocol.{GetUserListReq,ShowUserInfo}
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

  def updateListInfo(l:Set[String],s:Int)={
    db.run(userInfoTableQuery.filter(m => m.userId.inSet(l)).map(m => m.state).update(s))
  }

  def getListNumByState(state:Option[Int])={
    val action=if (state.isEmpty) {
      userInfoTableQuery.size
    } else {
      userInfoTableQuery.filter(r => r.state === state.get).size
    }
    db.run(action.result)
  }

  def getUserList(req:GetUserListReq) = {
    val action=if (req.state.isEmpty) {
      userInfoTableQuery.sortBy(_.createTime).drop((req.page - 1) * req.pageNum).take(req.pageNum).map(u => (u.userId,u.mail,u.state,u.createTime))
    } else {
      userInfoTableQuery.filter(r => r.state === req.state.get).sortBy(_.createTime).drop((req.page - 1) * req.pageNum).take(req.pageNum).map(u => (u.userId,u.mail,u.state,u.createTime))
    }
    db.run(action.result)
  }

}
package com.neo.sk.breaker.shared.protocol

import com.neo.sk.breaker.shared.ptcl.CommonRsp

/**
  * Created by sky
  * Date on 2019/2/14
  * Time at 下午5:40
  */
object UserProtocol {

  case class UserInfo(
                       nickName: String,
                       playerId: Option[String], //游戏中的ID,注册用户和游客前缀不同
                       userName: Option[String] = None, //注册时的用户名
                     )

  case class UserLoginReq(
                           userId: String,
                           password: String
                         )

  case class UserSignReq(
                          userId: String,
                          mail: String,
                          password: String
                        )

  case class GetUserInfoRsp(
                             userType: Option[Int],
                             userId: Option[String],
                             errCode: Int = 0,
                             msg: String = "ok"
                           ) extends CommonRsp

  case class GetUserListReq(
                             state: Option[Int],
                             page: Int,
                             pageNum: Int
                           )

  case class ShowUserInfo(
                           userId: String,
                           mail: String,
                           state:Int,
                           createTime: Long
                         )

  case class GetUserListRsp(
                             totalPage: Option[Int],
                             data: Option[List[ShowUserInfo]],
                             errCode: Int = 0,
                             msg: String = "ok"
                           ) extends CommonRsp

  case class AddState4UserReq(
                          user:List[String],
                          state:Int
                          )

}

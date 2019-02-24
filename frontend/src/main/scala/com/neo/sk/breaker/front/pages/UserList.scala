package com.neo.sk.breaker.front.pages

import com.neo.sk.breaker.front.common.{Page, Routes}
import com.neo.sk.breaker.front.components.{CheckBox, PageMod}
import com.neo.sk.breaker.front.utils.{Http, TimeTool}
import com.neo.sk.breaker.shared.protocol.UserProtocol
import mhtml.{Var, emptyHTML}

import scala.concurrent.Future
import scala.xml.Elem
import org.scalajs.dom.html.Input
import io.circe.generic.auto._
import io.circe.syntax._

import scala.collection.mutable.HashMap
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by sky
  * Date on 2019/2/24
  * Time at 下午4:11
  */
object UserList {
  val listMap = HashMap[Option[Int], UserList]()
}

class UserList(state:Option[Int],checkCallBack: String => Unit = { _ => },
               uncheckCallBack: String => Unit = { _ => }) extends Page {

  val pageMod = new PageMod[UserProtocol.ShowUserInfo](obUserList)

  private val messageDiv: Var[String] = Var("none")

  def obUserList(page: Int, pageNum: Int): Future[(Option[Int], List[UserProtocol.ShowUserInfo])] = {
    messageDiv := "load"
    Http.postJsonAndParse[UserProtocol.GetUserListRsp](Routes.User.getUserList, UserProtocol.GetUserListReq(state, page, pageNum).asJson.noSpaces).map {
      rsp =>
        messageDiv := "warn"
        if (rsp.errCode == 0 && rsp.data.isEmpty) {
          (None, Nil)
        }
        else (rsp.totalPage, rsp.data.get)
    }
  }

  val userList =
    <div>
      {pageMod.indexData.map {
      case Nil =>
        <div style="width: 100%;display: table;min-height: 450px;">
          {messageDiv.map {
          case "load" =>
            <div style="text-align: center;display: table-cell;vertical-align: middle;">
              <img style="width:100px" src="/breaker/static/img/loading.gif"></img>
              <p style="font-family: PingFangSC-Regular;font-size: 16px;color: #909399;margin-top:10px;">加载中</p>
            </div>
          case "warn" =>
            //                Component.loadingDiv
            <div style="text-align: center;display: table-cell;vertical-align: middle;">
              <img style="width:100px" src="/breaker/static/img/无数据.png"></img>
              <p style="font-family: PingFangSC-Regular;font-size: 16px;color: #909399;margin-top:20px;">无数据</p>
            </div>
        }}
        </div>
      case list =>
        <div style="height:100%">
          <div style="height:80%">
            <table id="bot_table" style="width:100%">
              {list.map {
              info =>
                <tr style={if(info.state==1) "width:100%" else "background-color: beige;width:100%"} class="tr-line">
                  <td class="td-line-1">{CheckBox[String](UserListPage.choseList.contains(info.userId),info.userId,checkCallBack,uncheckCallBack).box}</td>

                  <td class="td-line-2">
                    {info.userId}
                  </td>

                  <td class="td-line-3">
                    {info.mail}
                  </td>

                  <td class="td-line-4">
                    {TimeTool.dateFormatDefault(info.createTime)}
                  </td>
                </tr>
            }}
            </table>
          </div>
          <div style="text-align: right;margin-right: 24px;">
            {pageMod.pageDiv}
          </div>
        </div>
    }}
    </div>

  pageMod.initPageInfo(10)

  override def render: Elem = {
    <div style="height:100%">
      {userList}
    </div>
  }

}


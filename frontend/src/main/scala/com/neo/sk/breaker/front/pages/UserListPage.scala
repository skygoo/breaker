package com.neo.sk.breaker.front.pages

import com.neo.sk.breaker.front.common.{Page, Routes}
import com.neo.sk.breaker.front.components.AlertModel
import com.neo.sk.breaker.front.utils.Http
import com.neo.sk.breaker.shared.protocol.UserProtocol.AddState4UserReq
import com.neo.sk.breaker.shared.ptcl.SuccessRsp
import mhtml.Var
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.Event

import scala.collection.mutable
import scala.xml.Elem
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by sky
  * Date on 2019/2/24
  * Time at 下午3:59
  */
object UserListPage extends Page {
  val stateFlagVar: Var[Option[Int]] = Var(None)
  var stateFlag: Option[Int] = None
  /** 选择用户 */
  val choseList = new mutable.HashSet[String]()

  /**
    * 刷新页面到0
    *
    * @param state 是否停留原页码*/
  def refreshPage(state: Boolean = false) = {
    choseList.clear()
    UserList.listMap.getOrElseUpdate(stateFlag, new UserList(stateFlag)).pageMod.initPageInfo(10, state)
  }

  def checkCallBack(openId: String) = {
    println("add", openId, choseList.size)
    choseList.add(openId)
  }

  def uncheckCallBack(openId: String) = {
    println("remove", openId, choseList.size)
    choseList.remove(openId)
  }

  private val filterResultDiv = stateFlagVar.map { r =>
    stateFlag = r
    choseList.clear()
    UserList.listMap.getOrElseUpdate(r, new UserList(r, checkCallBack, uncheckCallBack)).render
  }

  override def render: Elem =
    <div>
      <div id="filter-main-middle">
        <div style="text-align: left;">
          <div class="filter-middle-r-c">
            <span style="padding: 6px 13px 6px 13px;margin-right: 2%;" class="button-all" onclick={() =>addState4User(2)}>封禁操作</span>
          </div>
          <div class="filter-middle-r-c">
            <span style="padding: 6px 13px 6px 13px;margin-right: 2%;" class="button-all" onclick={() =>addState4User(1)}>解禁操作</span>
          </div>
          <div class="filter-middle-r-c" style="float:right" onmouseleave={() => StateListModel.showQRCode := false}>
            <span class="down-button-div" onclick={() => StateListModel.showQRCode := true}>
              {StateListModel.choseVar.map(r => r._2)}<img class="down-button-img" src={Routes.getImgUrl("下拉.png")}></img>
            </span>{StateListModel.QRCodeBox}
          </div>
        </div>
      </div>
      <hr class="hr-line"></hr>
      <div id="filter-main-bottom">
        {filterResultDiv}
      </div>
    </div>

  private def addState4User(state:Int):Unit=
    if(choseList.nonEmpty){
      Http.postJsonAndParse[SuccessRsp](Routes.User.addState4User, AddState4UserReq(choseList.toList,state).asJson.noSpaces).map {
        rsp =>
          if (rsp.errCode == 0) {
            //remind 清除页面缓存
            UserList.listMap.remove(Some(state))
            AlertModel.mainYAlert.show(if (state==1) "解禁成功" else "封禁成功", 1500)
            choseList.clear()
          } else {
            AlertModel.mainYAlert.show(if (state==1) "解禁:"+rsp.msg else "封禁:"+rsp.msg, 1500)
          }
          refreshPage(true)
      }
    }else{
      MainPage.createConfirm("请选择用户")
    }


}

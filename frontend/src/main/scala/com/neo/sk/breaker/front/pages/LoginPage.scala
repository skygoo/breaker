package com.neo.sk.breaker.front.pages

import com.neo.sk.breaker.front.common.{Page, Routes}
import com.neo.sk.breaker.front.utils.{Http, Shortcut}
import com.neo.sk.breaker.shared.model.Constants
import com.neo.sk.breaker.shared.protocol.UserProtocol.{GetUserInfoRsp, UserInfo, UserLoginReq, UserSignReq}
import com.neo.sk.breaker.shared.ptcl.SuccessRsp
import mhtml.Var
import mhtml.emptyHTML
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.html.Input

import scala.util.Random
import scala.xml.Elem
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.ExecutionContext.Implicits.global
import com.neo.sk.breaker.shared.model.Constants.RoomType
/**
  * Created by sky
  * Date on 2019/1/30
  * Time at 上午9:56
  */
object LoginPage extends Page {
  var roomType=RoomType.confrontation
  val random = new Random(System.currentTimeMillis())
  val fromContentFlagVar = Var(0)

  val guestName = List("安琪拉", "白起", "不知火舞", "妲己", "狄仁杰", "典韦", "韩信", "老夫子", "刘邦",
    "刘禅", "鲁班七号", "墨子", "孙膑", "孙尚香", "孙悟空", "项羽", "亚瑟", "周瑜",
    "庄周", "蔡文姬", "甄姬", "廉颇", "程咬金", "后羿", "扁鹊", "钟无艳", "小乔", "王昭君",
    "虞姬", "李元芳", "张飞", "刘备", "牛魔王", "张良", "兰陵王", "露娜", "貂蝉", "达摩", "曹操",
    "芈月", "荆轲", "高渐离", "钟馗", "花木兰", "关羽", "李白", "宫本武藏", "吕布", "嬴政",
    "娜可露露", "武则天", "赵云", "姜子牙", "哪吒", "诸葛亮", "黄忠", "大乔", "东皇太一",
    "庞统", "干将莫邪", "鬼谷子", "女娲", "SnowWhite", "Cinderella", "Aurora", "Ariel", "Belle", "Jasmine",
    "Pocahontas", "Mulan", "Tiana", "Rapunzel", "Merida", "Anna", "Elsa", "Moana")

  def getRandomName: String = {
    guestName(random.nextInt(guestName.length))
  }

  var playerInfo: UserInfo = UserInfo(getRandomName, None, None)

  val userIdVar:Var[Option[String]]=Var(None)

  def login(): Unit = {
    val userId = dom.document.getElementById("loginId").asInstanceOf[Input].value
    val psw = dom.document.getElementById("loginPassword").asInstanceOf[Input].value
    if(userId!=""&&psw!=""){
      val url = Routes.User.login
      val data = UserLoginReq(userId, psw).asJson.noSpaces
      Http.postJsonAndParse[SuccessRsp](url, data).map {
        rsp =>
          if (rsp.errCode == 0) {
            userIdVar:=Some(userId)
            playerInfo=playerInfo.copy(userName=Some(userId))
            fromContentFlagVar := 0
          } else {
            MainPage.createConfirm(rsp.msg)
          }
      }
    }else{
      MainPage.createConfirm("输入不可为空")
    }
  }

  def logout(): Unit = {
    Http.getAndParse[SuccessRsp](Routes.User.logout).map {
      rsp =>
        if (rsp.errCode == 0) {
          userIdVar:=None
          playerInfo=playerInfo.copy(userName=None)
          fromContentFlagVar := 0
        } else {
          MainPage.createConfirm(rsp.msg)
        }
    }
  }

  def signUp():Unit = {
    val userId = dom.document.getElementById("userId").asInstanceOf[Input].value
    val mail = dom.document.getElementById("userEmail").asInstanceOf[Input].value
    val password = dom.document.getElementById("userPassword").asInstanceOf[Input].value
    val passwordRe = dom.document.getElementById("userPasswordReEnter").asInstanceOf[Input].value
    if(userId!=""&&mail!=""&&password!=""&&passwordRe!=""){
      if (password.equals(passwordRe)) {
        val data = UserSignReq(userId, mail, password).asJson.noSpaces
        Http.postJsonAndParse[SuccessRsp](Routes.User.sign, data).map {
          rsp =>
            if (rsp.errCode == 0) {
              userIdVar:=Some(userId)
              playerInfo=playerInfo.copy(userName=Some(userId))
              fromContentFlagVar := 0
            } else {
              MainPage.createConfirm(rsp.msg)
            }
        }
      }else{
        MainPage.createConfirm("密码输入不一致")
      }
    }else{
      MainPage.createConfirm("输入不可为空")
    }
  }

  def start = {
    Shortcut.redirect(s"#/play/$roomType")
  }

  val filterMiddleDiv = fromContentFlagVar.map {
    case 0 =>
      <div class="filter-main-middle-r">
        <div id="general-css-word" style="display:inline-block;letter-spacing: 0.26px;line-height: 22px;color: #1D2341;">昵称</div>
        <input id="createTagInput" placeholder="请选择昵称" value={playerInfo.nickName} maxlength="30"
               style="background: #FFFFFF;border: 1px solid rgba(142,152,180,0.50);border-radius: 4px;height: 32px;padding-right:55px" oninput={e: Event =>
          dom.document.getElementById("createTagInput").asInstanceOf[Input].style.border = "1px solid rgba(142,152,180,0.50)"
          val elem = e.target.asInstanceOf[Input]
          playerInfo = playerInfo.copy(nickName = elem.value)}></input>
        <span style="display:inline-block;width:20px;top:115px;left:360px;font-family: PingFangSC-Regular;font-size: 14px;color: #D9DFEB;letter-spacing: 0.26px;line-height: 22px;">
          <img src={Routes.getImgUrl("dice.png")} style="width:100%" onclick={() =>
            playerInfo = playerInfo.copy(nickName = getRandomName)
            val elem = dom.document.getElementById("createTagInput").asInstanceOf[Input]
            elem.value = playerInfo.nickName}></img>
        </span>
        <div class="button-all" style="width:94px;margin: auto;margin-top:30px;" onclick={() => start}>开始游戏</div>
        {userIdVar.map{r=>if(r.nonEmpty) <div class="button-all" style="width:94px;margin: auto;margin-top:30px;" onclick={() => logout()}>退出登录</div> else emptyHTML }}
      </div>
    case 1 =>
      <div class="filter-main-middle-r">
        <div class="form-content" style="width:80%;margin: auto;">
          <input class="form-control input_class" id="loginId" placeholder="userId"></input>
          <input type="password" class="form-control input_class" id="loginPassword" placeholder="password"></input>
        </div>
        <div class="form-submit input_class">
          <div class="button-all" style="width:94px;margin: auto;" onclick={() => login()}>登录</div>
        </div>
        <div class="form-tip input_class">
          <span>如果你还没有邮箱账号
            <a onclick={() => fromContentFlagVar := 2} style="cursor:pointer;">点击这里</a>
          </span>
        </div>
        <div class="form-information">
        </div>
      </div>
    case 2 =>
      <div class="filter-main-middle-r">
        <div class="form-content" style="width:80%;margin: auto;">
          <input class="form-control input_class" id="userId" placeholder="userId(8字内)" maxlength="8"></input>
          <input class="form-control input_class" id="userEmail" placeholder="email"></input>
          <input type="password" class="form-control input_class" id="userPassword" placeholder="password"></input>
          <input type="password" class="form-control input_class" id="userPasswordReEnter" placeholder="re_enter password"></input>
        </div>
        <div class="form-submit input_class">
          <div class="button-all" style="width:94px;margin: auto;" onclick={() => signUp()}>注册</div>
        </div>
        <div class="form-tip input_class">
          <span>如果你已经注册过了
            <a onclick={() => fromContentFlagVar := 1} style="cursor:pointer;">点击这里</a>
          </span>
        </div>
        <div class="form-information">
        </div>
      </div>
    case _ => emptyHTML
  }

  override def render: Elem = {
    <div style="width:100%;height:100%;">
      <div id="filter-main">
        <div>
          <div id="filter-main-header">
            <span class={fromContentFlagVar.map {
              case 0 => "filter-header-a-on"
              case _ => "filter-header-a"
            }} onclick={() => fromContentFlagVar := 0}>{userIdVar.map(r=>if(r.nonEmpty)r.get else "游客")}</span>
            <span class={fromContentFlagVar.map {
              case 1 => "filter-header-a-on"
              case _ => "filter-header-a"
            }} onclick={() => fromContentFlagVar := 1}>登录</span>
            <span class={fromContentFlagVar.map {
              case 2 => "filter-header-a-on"
              case _ => "filter-header-a"
            }} onclick={() => fromContentFlagVar := 2}>注册</span>
          </div>
          <div>
            {filterMiddleDiv}
          </div>
        </div>
      </div>
    </div>
  }
}

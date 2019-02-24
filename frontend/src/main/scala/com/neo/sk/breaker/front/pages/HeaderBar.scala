package com.neo.sk.breaker.front.pages

import com.neo.sk.breaker.front.common.{Component, Routes}
import com.neo.sk.breaker.front.utils.{Http, Shortcut}
import com.neo.sk.breaker.shared.protocol.UserProtocol.GetUserInfoRsp
import com.neo.sk.breaker.shared.ptcl.SuccessRsp
import org.scalajs.dom
import mhtml._
import org.scalajs.dom.raw.MouseEvent
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Elem

/**
  * Created by hongruying on 2018/12/10
  *
  * @author sky 修改
  */
object HeaderBar extends Component{

  val adminIdVar:Var[Option[String]]=Var(None)

  private val userDivShowStyle = adminIdVar.map{
    case None =>
      Shortcut.redirect("#/admin/login")
      "display: block;position: absolute;display:none;"
    case _ =>
      Shortcut.redirect("#/admin/userList")
      "display: block;position: absolute;display:block;"
  }

  private val isShow = Var(false)

  private val logoutDisplay = isShow.map{
    case true => "display:block;"
    case false => "display:none"
  }

  private val logoutDiv =
    <ul class ="logoutSub" style={logoutDisplay}>
      <li style="margin-top: 10px;text-align: left;" onclick={() => logout()}>
        <img src ={Routes.getImgUrl("退出.png")} ></img>
        <div style="display:inline-block;">
          退出登录
        </div>
      </li>
    </ul>


  def logout(): Unit = {
    Http.getAndParse[SuccessRsp](Routes.Admin.logout).map {
      rsp =>
        if (rsp.errCode == 0) {
          adminIdVar :=None
        } else {
          MainPage.createConfirm(rsp.msg)
        }
    }
  }




  private def showLogout(e: MouseEvent) = {
    e.preventDefault()
    isShow := true
  }

  private def unshowLogout(e: MouseEvent) = {
    e.preventDefault()
    isShow := false
  }

  //<div class ="userDiv" onmouseover ={e:MouseEvent => showLogout(e)} onmouseout ={e:MouseEvent => unshowLogout(e)}>
  //
  private val userIcon =
  <div style ={userDivShowStyle} onmouseover ={e:MouseEvent => showLogout(e)} onmouseout ={e:MouseEvent => unshowLogout(e)}>
    <div class="user">
      <span class ="username">{adminIdVar}</span>
      <div class ="icon"></div>
    </div>
    {logoutDiv}
  </div>




  override def render: Elem =
    <div class ="headerContainer">
      <div class ="header" style={s"margin-left: 30px;"}>
        <div>
          <img src ={Routes.getImgUrl("瓷砖.png")} style="width:45px;"></img>
          <span class ="header_title">数据后台</span>
        </div>
        <div style="width: 15%;">
          {userIcon}
        </div>
      </div>
    </div>

}

package com.neo.sk.breaker.front.components

import com.neo.sk.breaker.front.common.Component
import com.neo.sk.breaker.shared.protocol.UserProtocol.UserInfo
import com.neo.sk.breaker.shared.model.Constants.GameState
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.KeyboardEvent
import scala.xml.Elem
import com.neo.sk.breaker.front.utils.Shortcut

/**
  * Created by hongruying on 2018/7/9
  */
class StartGameModal(gameState:Var[Int],startGame:(String,Option[Long]) => Unit, playerInfoOpt:Option[UserInfo]) extends Component{


  private val inputDisabled:Var[Boolean] = Var(playerInfoOpt.isDefined)
  private val inputValue:Var[String] = Var(playerInfoOpt.getOrElse(""))
//  private val input4RoomIdValue:Var[Option[Long]] = Var(playerInfoOpt.map(_.roomIdOpt).get)
  private var inputName = ""

  private val title = gameState.map{
    case GameState.firstCome => "欢迎来到坦克大战io，请输入用户名进行游戏体验"
    case GameState.stop => "重新开始"
    case _ => ""
  }

  private val divStyle = gameState.map{
    case GameState.firstCome => "display:block;"
    case GameState.stop => "display:block;"
    case _ => "display:none;"
  }

  private val inputDivStyle = gameState.map{
    case GameState.firstCome => "display:block;"
    case _ => "display:none;"
  }

  private val watchButtonDivStyle = inputDisabled.map{
    case true => "display:none;"
    case false => "display:inline;"
  }


  private val inputElem = <input id ="breakerGameNameInput" onkeydown ={e:KeyboardEvent => clickEnter(e)} disabled={inputDisabled} value ={inputValue}></input>
  private val inputElem4RoomId = <input id="breakerGameRoomIdInput" onkeydown={e:KeyboardEvent => clickEnter(e)} disabled={inputDisabled}></input>
  private val button = <button id="start_button" class ="btn btn-info" onclick ={() => clickEnter()}>进入游戏</button>
  private val watchButton = <button id="watch_button" class ="btn btn-info" onclick ={() => Shortcut.redirect("#/getGameRec")}>观看列表</button>



  def clickEnter(e:KeyboardEvent):Unit = {
    if(e.keyCode == KeyCode.Enter){
      val name = dom.document.getElementById("breakerGameNameInput").asInstanceOf[dom.html.Input].value
      val roomIdString = dom.document.getElementById("breakerGameRoomIdInput").asInstanceOf[dom.html.Input].value
      val roomIdOpt = if(roomIdString == "") None else Some(roomIdString.toLong)
      if(name.nonEmpty){
        inputName = name
        startGame(name,roomIdOpt)
      }
      e.preventDefault()
    }
    if(e.keyCode == KeyCode.Space){
      if(inputName != "") startGame(inputName,None)
    }
  }


  def clickEnter():Unit = {
    val name = dom.document.getElementById("breakerGameNameInput").asInstanceOf[dom.html.Input].value
    val roomIdString = dom.document.getElementById("breakerGameRoomIdInput").asInstanceOf[dom.html.Input].value
    val roomIdOpt = if(roomIdString == "") None else Some(roomIdString.toLong)
    if(name.nonEmpty){
      inputName = name
      startGame(name,roomIdOpt)
    }
  }

  override def render: Elem = {
    <div style={divStyle}>
      <div class ="input_mask" id="input_mask_id" tabindex="-1" onkeydown ={e:KeyboardEvent => clickEnter(e)}></div>
      <div class ="input_div" style={inputDivStyle}>
        <div class ="input_title">{title}</div>
        <div>
          <p class="input_inline"><span class="input_des">名字</span>{inputElem}</p>
          <p class="input_inline"><span class="input_des">房间</span>{inputElem4RoomId}</p>
        </div>
        <div class ="input_button">
          <span>{button}</span>
          <span style={watchButtonDivStyle}>{watchButton}</span>
        </div>
      </div>
    </div>

  }


}

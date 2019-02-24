package com.neo.sk.breaker.front.pages

import com.neo.sk.breaker.front.components.model.RelativeModel
import mhtml.Var

/**
  * Created by sky
  * Date on 2018/12/19
  * Time at 下午5:03
  */
object StateListModel extends RelativeModel("top: 30px;left: 0%;") {
  private val activeList = List((0, "全部"), (1, "活跃"), (2, "封禁"))
  val choseVar = Var((0, "全部"))
  override val modelDiv = <div>
    {choseVar.map(r =>
      <div>
        {activeList.map(l =>
        <a class="list-item" onclick={() =>
          UserListPage.stateFlagVar := (if (l._1 == 0) None else Some(l._1))
          choseVar := l
          UserListPage.refreshPage()
          cancelShowQR}>
          <p style="word-wrap: break-word;">
            {l._2}
          </p>
        </a>
      )}
      </div>
    )}
  </div>

  override def cancelShowQR = {
    showQRCode := false
  }
}


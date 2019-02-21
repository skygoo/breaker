package com.neo.sk.utils

import org.slf4j.LoggerFactory
import com.zaxxer.hikari.HikariDataSource
import slick.jdbc.H2Profile

/**
  * Created by dry on 2018/10/24.
  *
  */
object H2DBUtil {

  val log = LoggerFactory.getLogger(this.getClass)
  private val dataSource = createDataSource()

  import com.neo.sk.breaker.common.AppSettings._

  private def createDataSource() = {

    val dataSource = new org.h2.jdbcx.JdbcDataSource
    dataSource.setURL(slickUrl)
    dataSource.setUser(slickUser)
    dataSource.setPassword(slickPassword)
    val hikariDS = new HikariDataSource()
    hikariDS.setDataSource(dataSource)
    hikariDS.setMaximumPoolSize(slickMaximumPoolSize)
    hikariDS.setConnectionTimeout(slickConnectTimeout)
    hikariDS.setIdleTimeout(slickIdleTimeout)
    hikariDS.setMaxLifetime(slickMaxLifetime)
    hikariDS.setAutoCommit(true)
    hikariDS
  }

  val driver = H2Profile

  import driver.api.Database

  val db: Database = Database.forDataSource(dataSource, Some(slickMaximumPoolSize))

}

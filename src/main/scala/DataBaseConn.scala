import DataBaseConn.xa
import cats.effect.{Blocker, IO}
import com.zaxxer.hikari.HikariDataSource
import doobie._
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.implicits.javasql._

import java.sql.Timestamp
import scala.concurrent.ExecutionContext

trait UrlDatabaseFunctions {
  def storeUrl(userId: Int, timestamp: Timestamp, value: String): Int
  def getAllUrls: List[Url]
  def getAllUrlsForUser(userId: Int): List[Url]
  def getUrlById(urlId: Int): Option[Url]
  def getUrlByUserIdAndUrlId(urlId: Int): Option[Url]
}

object UrlDatabaseImplementation extends UrlDatabaseFunctions {

  def storeUrl(userId: Int, timestamp: Timestamp, value: String): Int = {
    sql"insert into urls(timestamp, value, userid) values ($timestamp, $value, $userId)"
      .update
      .run
      .transact(xa)
      .unsafeRunSync()
  }

  def getAllUrls: List[Url] = {
    sql"select userid, value, timestamp from urls".query[Url]
      .to[List]
      .transact(xa)
      .unsafeRunSync()
  }

  def getAllUrlsForUser(userId: Int): List[Url] = {
    sql"select userid, value, timestamp from urls where userid=$userId".query[Url]
      .to[List]
      .transact(xa)
      .unsafeRunSync()
  }

  def getUrlById(urlId: Int): Option[Url] = {
    sql"select userid, value, timestamp from urls where id=$urlId".query[Url]
      .option
      .transact(xa)
      .unsafeRunSync()

  }

  def getUrlByUserIdAndUrlId(urlId: Int): Option[Url] = {
    sql"select userid, value, timestamp from urls where id=$urlId".query[Url]
      .option
      .transact(xa)
      .unsafeRunSync()
  }
}

trait UserDatabaseFunctions{
  def findUserByName(username: String): Option[User]
  def getAll: List[UserRecord]
  def insertUser(username: String, password: String): Int
  def updateUsername(usernameFirst: String, usernameSecond: String): Int
  def findUser(username: String): Option[User]
}

object UserDatabaseImplementation extends UserDatabaseFunctions {

  override def findUserByName(username: String): Option[User]  = {
    sql"select * from users where username = $username"
      .query[User]
      .option
      .transact(xa)
      .unsafeRunSync()
  }

  override def getAll: List[UserRecord]  = {
    sql"select id, username, password from users"
      .query[UserRecord]
      .to[List]
      .transact(xa)
      .unsafeRunSync()
  }

  override def insertUser(username: String, password: String): Int = {
    sql"insert into users(username, password) values ($username, $password)"
      .update
      .run
      .transact(xa)
      .unsafeRunSync()
  }

  override def updateUsername(usernameFirst: String, usernameSecond: String): Int = {
    sql"update users set username = $usernameSecond where username=$usernameFirst"
      .update
      .run
      .transact(xa)
      .unsafeRunSync()
  }

  override def findUser(username: String): Option[User] = {
    sql"select username, password from users where username = $username"
      .query[User]
      .option
      .transact(xa)
      .unsafeRunSync()
  }
}

object DataBaseConn {
  implicit val cs = IO.contextShift(ExecutionContext.global)

  private val dataSource: HikariDataSource = {
    val ds = new HikariDataSource
    ds.setDriverClassName("org.postgresql.Driver")
    ds.setJdbcUrl("jdbc:postgresql:browser_history")
    ds.setUsername("postgres")
    ds.setPassword("")
    ds
  }

  val xa: Transactor[IO] = HikariTransactor[IO](
    dataSource,
    connectEC = ExecutionContexts.synchronous,
    blocker = Blocker.liftExecutionContext(ExecutionContexts.synchronous)
  )

}

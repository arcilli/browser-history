import DataBaseConn.xa
import cats.effect.IO
import doobie._
import doobie.implicits._
import scala.concurrent.ExecutionContext

trait UserDatabaseFunctions{
  def findUserByName(username: String): Option[User]
  def getAll: List[UserRecord]
  def insertUser(username: String, password: String)
  def updateUsername(usernameFirst: String, usernameSecond: String)
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

  override def insertUser(username: String, password: String) = {
    sql"insert into users(username, password) values ($username, $password)"
      .update
      .run
      .transact(xa)
      .unsafeRunSync()
  }

  override def updateUsername(usernameFirst: String, usernameSecond: String) = {
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

object DataBaseConn{
  implicit val cs = IO.contextShift(ExecutionContext.global)

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:browserhistory", "postgres", ""
  )

}

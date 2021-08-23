
//DataBaseConnection

object DataBaseConn{
  import cats.effect.IO
  import doobie._
  import doobie.implicits._

  import scala.concurrent.ExecutionContext

  implicit val cs = IO.contextShift(ExecutionContext.global)

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:browserhistory", "postgres", ""
  )

  def findUserByName(username: String)  = {
    sql"select * from users where username = $username"
      .query[User]
      .option
      .transact(xa)
      .unsafeRunSync()
  }

  def getAll: List[UserRecord]  = {
    sql"select id, username, password from users"
      .query[UserRecord]
      .to[List]
      .transact(xa)
      .unsafeRunSync()
  }

  def insertUser(username: String, password: String) = {
    sql"insert into users(username, password) values ($username, $password)"
      .update
      .run
      .transact(xa)
      .unsafeRunSync()
  }

  def updateUsername(usernameFirst: String, usernameSecond: String) = {
    sql"update users set username = $usernameSecond where username=$usernameFirst"
      .update
      .run
      .transact(xa)
      .unsafeRunSync()
  }

  def findUser(username: String, password: String) ={
    sql"select * from users where username = $username and password = $password"
      .query[User]
      .option
      .transact(xa)
      .unsafeRunSync()
  }

}

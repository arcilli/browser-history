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

//  def find(n: String)  =
//    sql"select id, username, password from users where username = $n"
//      .query[UserComplete]
//      .option
//      .transact(xa)
//      .unsafeRunSync()

  def getAll: List[UserComplete]  = {
    val users = sql"select id, username, password from users"
      .query[UserComplete]
      .to[List]
      .transact(xa)
      .unsafeRunSync()
    println(users)
    users
  }

  def putUser(username: String, password: String) = {
    sql"insert into users(username, password) values ($username, $password)"
      .update.run.transact(xa).unsafeRunSync()
  }

}

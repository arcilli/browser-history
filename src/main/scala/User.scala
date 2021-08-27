import spray.json._

import scala.language.postfixOps

/*
  POST /register -> register User(username, password)
  GET /users -> get all the users
  GET /user/{id} -> get a user by id
  GET /login -> OK (Successful), NotFound (Error)
  PATCH /changeName?name1=?&name2=?
 */

case class User(username: String, password: String)
case class UserRecord(id: Int, username: String, password: String)

trait UserJsonProtocol extends DefaultJsonProtocol {
  implicit val userPartialFormat = jsonFormat2(User)
  implicit val userCompleteFormat = jsonFormat3(UserRecord)
}

object UserRoute extends UserJsonProtocol {

  def registerHashPass(password: String): String = password.hashCode.toString

}

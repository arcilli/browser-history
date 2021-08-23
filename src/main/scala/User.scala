import UserDatabaseImplementation._
import UserRoute.registerAndLoginRoute
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
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

trait ActorUser{
  implicit val system = ActorSystem("UserActor")
}

object UserRoute extends UserJsonProtocol with ActorUser with SprayJsonSupport {

  def findUserById(idUser: Int) = {
    getAll.find(i => i.id == idUser)
  }

  def registerHashPass(password: String): String = password.hashCode.toString

  val registerAndLoginRoute =
    (path("register") & post){
      entity(as[User]){ user =>
        insertUser(user.username, registerHashPass(user.password))
        complete(StatusCodes.OK)
      }
    }~
      (path("users") & get){
        complete(
          HttpEntity(
            ContentTypes.`application/json`,
            getAll.toJson.prettyPrint
          )
        )
      }~
      (path("user" / IntNumber) & get){ userId =>
        val maybeId: Option[UserRecord] = findUserById(userId)
        maybeId match {
          case Some(item) =>
            complete(
              HttpEntity(
                ContentTypes.`application/json`,
                item.toJson.prettyPrint
              )
            )
          case None => complete(StatusCodes.NotFound)
        }
      }~
      (path("login") & get) {
        entity(as[User]){ user: User =>
          val userFound = findUser(user.username)
          if(userFound.isDefined) {
            if(registerHashPass(user.password).equals(userFound.get.password))
              complete(StatusCodes.OK)
            else complete(StatusCodes.NotFound)
          } else complete(StatusCodes.NotFound)
        }
      }~
      (path( "changeName") & patch) {
        parameter(Symbol("name1").as[String], Symbol("name2").as[String]) { (name1, name2) =>
          val maybeId: Option[User] = findUserByName(name1)
          maybeId match {
            case Some(_) =>
              updateUsername(name1, name2)
              complete(StatusCodes.OK)
            case None => complete(StatusCodes.NotFound)
          }
        }
      }

}

object MainUser extends App with ActorUser {

  Http().newServerAt("localhost", 8080).bind(registerAndLoginRoute)
}

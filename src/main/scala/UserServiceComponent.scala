import UserRoute.registerHashPass
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json._

trait UserServiceComponent {
  this: UserRepositoryComponent =>

  val userService: UserService

  class UserService extends UserJsonProtocol with SprayJsonSupport {
    val registerAndLoginRoute: Route = {
      (path("register") & post) {
        entity(as[User]) { user =>
          if (userRepository.insertUser(user.username, registerHashPass(user.password)).equals(1))
            complete(StatusCodes.OK)
          else
            complete(StatusCodes.InternalServerError)
        }
      } ~
        (path("users") & get) {
          complete(
            HttpEntity(
              ContentTypes.`application/json`,
              userRepository.getAll.toJson.prettyPrint
            )
          )
        } ~
        (path("user" / IntNumber) & get) { userId =>
          userRepository.findUserById(userId) match {
            case Some(item) =>
              complete(
                HttpEntity(
                  ContentTypes.`application/json`,
                  item.toJson.prettyPrint
                )
              )
            case None => complete(StatusCodes.NotFound)
          }
        } ~
        (path("login") & get) {
          entity(as[User]) { user: User =>
            userRepository.findUserByName(user.username) match {
              case Some(userFound) =>
                if (registerHashPass(user.password).equals(userFound.password))
                  complete(StatusCodes.OK)
                else complete(StatusCodes.Forbidden)
              case None => complete(StatusCodes.NotFound)
            }
          }
        } ~
        (path("changeName") & patch) {
          parameter(Symbol("name1").as[String], Symbol("name2").as[String]) { (name1, name2) =>
            userRepository.findUserByName(name1) match {
              case Some(_) =>
                if (userRepository.updateUsername(name1, name2).equals(1))
                  complete(StatusCodes.OK)
                else complete(StatusCodes.InternalServerError)

              case None => complete(StatusCodes.NotFound)
            }
          }
        }
    }
  }

}

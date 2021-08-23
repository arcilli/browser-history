import ServerSetup.route
import UrlDatabaseImplementation._
import UserDatabaseImplementation._
import UserRoute._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import spray.json._

trait SetupActor {
  implicit val system = ActorSystem("SetupActor")
}

object ServerSetup extends UrlJsonProtocol with SprayJsonSupport {

  val urlRoute =
    path("urls") {
      post {
        entity(as[Url]) { url =>
          storeUrl(url.userId, url.timestamp, url.value)
          complete(StatusCodes.OK)
        }
      } ~
        get {
          (path(IntNumber) | parameter(Symbol("urlid").as[Int])) { urlId =>
            getUrlById(urlId) match {
              case Some(url) => complete(
                HttpEntity(
                  ContentTypes.`application/json`,
                  url.toJson.prettyPrint
                )
              )
              case None => complete(StatusCodes.NotFound)
            }
          } ~
            parameter(Symbol("userid").as[Int]) { userId =>
              val urls = getAllUrlsForUser(userId)
              if (urls.isEmpty)
                complete(HttpEntity(
                  ContentTypes.`text/html(UTF-8)`,
                  """
                    |<html>
                    | <body>
                    |   User with the given ID doesn't have any URLs stored in the browser's history
                    | </body>
                    |</html>
                    |""".stripMargin
                ))
              else
                complete(
                  HttpEntity(
                    ContentTypes.`application/json`,
                    urls.toJson.prettyPrint
                  ))
            } ~
            pathEndOrSingleSlash {
              complete(
                HttpEntity(
                  ContentTypes.`application/json`,
                  getAllUrls.toJson.prettyPrint
                ))
            }
        }
    }

  val registerAndLoginRoute = {
      (path("register") & post) {
        entity(as[User]) { user =>
          insertUser(user.username, registerHashPass(user.password))
          complete(StatusCodes.OK)
        }
      } ~
        (path("users") & get) {
          complete(
            HttpEntity(
              ContentTypes.`application/json`,
              getAll.toJson.prettyPrint
            )
          )
        } ~
        (path("user" / IntNumber) & get) { userId =>
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
        } ~
        (path("login") & get) {
          entity(as[User]) { user: User =>
            val userFound = findUser(user.username)
            if (userFound.isDefined) {
              if (registerHashPass(user.password).equals(userFound.get.password))
                complete(StatusCodes.OK)
              else complete(StatusCodes.NotFound)
            } else complete(StatusCodes.NotFound)
          }
        } ~
        (path("changeName") & patch) {
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

  val route =
    pathPrefix("api") {
      urlRoute ~
        registerAndLoginRoute
    }

}

object MainUser extends App with SetupActor {

  Http().newServerAt("localhost", 8080).bind(route)
}

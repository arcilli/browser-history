import ServerSetup.route
import UrlRepositoryImplementation._
import UserRepositoryImplementation._
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
          if (storeUrl(url.userId, url.timestamp, url.value).equals(1))
            complete(StatusCodes.OK)
          else
            complete(StatusCodes.InternalServerError)
        }
      } ~
        get {
          parameter(Symbol("urlid").as[Int]) { urlId =>
            getUrlById(urlId) match {
              case Some(url) =>
                complete(
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
                complete(StatusCodes.NotFound)
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
        if (insertUser(user.username, registerHashPass(user.password)).equals(1))
          complete(StatusCodes.OK)
        else
          complete(StatusCodes.InternalServerError)
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
        findUserById(userId) match {
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
          findUserByName(user.username) match {
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
          findUserByName(name1) match {
            case Some(_) =>
              if (updateUsername(name1, name2).equals(1))
                complete(StatusCodes.OK)
              else complete(StatusCodes.InternalServerError)

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

object Main extends App with SetupActor {

  Http().newServerAt("localhost", 8080).bind(route)
}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import spray.json._

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}

/*
  POST /register -> register User(username, password)
  GET /users -> get all the users
  GET /user/{id} -> get a user by id
  GET /login -> OK (Successful), NotFound (Error)
 */

case class UserInfo(username: String, password: String)

trait UserJsonProtocol extends DefaultJsonProtocol{
  implicit val userFormat = jsonFormat2(UserInfo)
}

object User extends App with UserJsonProtocol{

  implicit val system = ActorSystem("User")
  import system.dispatcher

  var users = Map[Int, UserInfo]()
  var id: Int = 0

  def findUser(username: String, password: String): Boolean = {
    if(!(users.values.toList.find(i => i.username == username && i.password == registerHashPass(password)).isEmpty)) true
    else false
  }

  def findUserById(id: Int): Option[UserInfo] ={
    users.get(id)
  }
  def registerHashPass(password: String): String = password.hashCode.toString

  val registerRoute =
    (path("register") & post & extractRequest & extractLog){ (request, log) =>
      val entity = request.entity
      val strictEntity = entity.toStrict(2 seconds)
      val userFuture = strictEntity.map(_.data.utf8String.parseJson.convertTo[UserInfo])

      userFuture.onComplete{
        case Success(value) =>
          id += 1
          val user = UserInfo(value.username, registerHashPass(value.password))
          users = users + (id ->user)
        case Failure(exception) =>
          log.warning(s"Warning: $exception")
      }
      complete(userFuture
        .map(_ => StatusCodes.OK)
        .recover{
          case _ => StatusCodes.InternalServerError
        }
      )
    }~
      (path("users") & get){
        complete(
          HttpEntity(
            ContentTypes.`application/json`,
            users.values.toList.toJson.prettyPrint
          )
        )
      } ~
      (path("user" / IntNumber ) & get){ userId =>
        val maybeId: Option[UserInfo] = findUserById(userId)
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
      (path("login") & get & extractRequest & extractLog){ (request, log) =>
        val entity = request.entity
        val strictEntity = entity.toStrict(2 seconds)
        val userFuture = strictEntity.map(_.data.utf8String.parseJson.convertTo[UserInfo])

        if(findUser(userFuture.value.get.get.username, userFuture.value.get.get.password)) {
          userFuture.onComplete {
            case Success(value) =>
              log.info(s"Logged in with success: ${value.username}")
            case Failure(exception) =>
              log.warning(s"Warning: $exception")
          }
          complete(userFuture
            .map(_ => StatusCodes.OK)
            .recover{
              case _ => StatusCodes.InternalServerError
            }
          )
        }
        else
          complete(StatusCodes.NotFound)
      }

  Http().newServerAt("localhost", 8080).bind(registerRoute)
}

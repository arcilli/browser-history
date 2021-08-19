import DataBaseConn.{getAll, putUser, updateUsername}
import User.registerAndLoginRoute
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
  PATCH /changeName?name1=?&name2=?
 */

// id is auto incremented
case class UserComplete(id: Int, username: String, password: String)
case class UserPartial(username: String, password: String)

trait UserJsonProtocol extends DefaultJsonProtocol {
  implicit val userPartialFormat = jsonFormat2(UserPartial)
  implicit val userCompleteFormat = jsonFormat3(UserComplete)
}

trait ActorUser{
  implicit val system = ActorSystem("UserActor")
}

object User extends UserJsonProtocol with ActorUser {

  import system.dispatcher

  def checkLoginUser(username: String, password: String): Boolean = {
    if(!(getAll.find(i => i.username == username && i.password == password).isEmpty)) true
    else false
  }

  def findUserById(idUser: Int) = {
    getAll.find(i => i.id == idUser)
  }

  def registerHashPass(password: String): String = password.hashCode.toString

  val registerAndLoginRoute =
    (path("register") & post & extractRequest & extractLog){ (request, log) =>
      //TODO: try to use .map/for
      val entity = request.entity
      val strictEntity = entity.toStrict(2 seconds)
      val userFuture = strictEntity.map(_.data.utf8String.parseJson.convertTo[UserPartial])

      if(getAll.find(i => i.username == userFuture.value.get.get.username).isEmpty) {
        userFuture.onComplete {
          case Success(value) =>
            putUser(value.username, registerHashPass(value.password))
          case Failure(exception) =>
            log.error(s"Error: $exception")
        }
        complete(userFuture
          .map(_ => StatusCodes.OK)
          .recover {
            case _ => StatusCodes.InternalServerError
          }
        )
      }
      else complete(StatusCodes.Forbidden)
    }~
      (path("users") & get){
        complete(
          HttpEntity(
            ContentTypes.`application/json`,
            getAll.toJson.prettyPrint
          )
        )
      }~
      (path("user" / IntNumber ) & get){ userId =>
        val maybeId: Option[UserComplete] = findUserById(userId)
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
      (path("login") & get & extractRequest & extractLog) { (request, log) =>
        val entity = request.entity
        val strictEntity = entity.toStrict(2 seconds)
        val userFuture = strictEntity.map(_.data.utf8String.parseJson.convertTo[UserPartial])

        if (checkLoginUser(userFuture.value.get.get.username, registerHashPass(userFuture.value.get.get.password))) {
          userFuture.onComplete {
            case Success(value) =>
              log.info(s"Logged in with success: ${value.username}")
            case Failure(exception) =>
              log.error(s"Error: $exception")
          }
          complete(userFuture
            .map(_ => StatusCodes.OK)
            .recover {
              case _ => StatusCodes.InternalServerError
            }
          )
        }
        else
          complete(StatusCodes.NotFound)
      }~
  ///Change a name for a specific username
        (path( "changeName") & patch) {
            parameter(Symbol("name1").as[String], Symbol("name2").as[String]) { (name1, name2) =>
              val maybeId: Option[UserComplete] = getAll.find(i => i.username == name1)
                maybeId match {
                  case Some(_) =>
                    if(getAll.find(i => i.username == name2).isEmpty)
                    {
                      updateUsername(name1, name2)
                    complete(StatusCodes.OK)
                    }
                    else complete(StatusCodes.Forbidden)
                  case None => complete(StatusCodes.NotFound)
                }
              }
            }
}

object MainUser extends App with ActorUser {

  Http().newServerAt("localhost", 8080).bind(registerAndLoginRoute)
}

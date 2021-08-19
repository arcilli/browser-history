import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import cats.effect.IO

import scala.concurrent.duration._
import java.sql.Timestamp
import scala.language.postfixOps
import spray.json._
import doobie._
import doobie.implicits._
import doobie.implicits.javasql._


import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

case class UrlIn(userId: Int, value: String, timestamp: String)
case class UrlOut(userId: Int, value: String, timestamp: Timestamp)

object DatabaseConnection {
  implicit val cs = IO.contextShift(ExecutionContext.global)

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:browser_history", "postgres", ""
  )

  def createUrl(userId: Int, timestamp: String, value: String) = {
    sql"insert into urls(timestamp, value, userid) values (${Timestamp.valueOf(timestamp)}, $value, $userId)"
      .update
      .run
      .transact(xa)
      .unsafeRunSync()
  }

  def getAllUrls: List[UrlOut] = {
    sql"select userid, value, timestamp from urls".query[UrlOut]
      .to[List]
      .transact(xa)
      .unsafeRunSync()
  }
}

trait UrlJsonProtocol extends DefaultJsonProtocol {
  // for json url with timestamp string
  implicit val urlFormat = jsonFormat3(UrlIn)

  // for url with timestamp Timestamp
  implicit object TimestampFormat extends JsonFormat[Timestamp] {
    override def write(obj: Timestamp): JsValue =  JsString(obj.toString)

    override def read(json: JsValue): Timestamp =
      json match {
        case JsString(time) =>  Timestamp.valueOf(time)
        case _ => throw DeserializationException("Expected Timestamp as JsNumber")
      }
  }
  implicit val urlFormatOut = jsonFormat3(UrlOut)
}

object MainUrls extends App with UrlJsonProtocol with SprayJsonSupport {
  implicit val system = ActorSystem("Url")
  import system.dispatcher
  import DatabaseConnection._

  implicit val timeout = Timeout(2 seconds)

  val route =
    path("api" / "urls") {
      (post & extractRequest) { request =>
        val entity = request.entity
        val strictEntityFuture = entity.toStrict(2 seconds)
        val urlFuture = strictEntityFuture.map (_.data.utf8String.parseJson.convertTo[UrlIn])

        onComplete(urlFuture) {
          case Success(url) =>
            createUrl(url.userId,  url.timestamp, url.value)
            complete(StatusCodes.OK)
          case Failure(ex) =>
            failWith(ex)
        }
      } ~
        get {
          pathEndOrSingleSlash {
            complete(
              HttpEntity(
                ContentTypes.`application/json`,
                getAllUrls.toJson.prettyPrint
              ))
          }
        }
    }

  Http().newServerAt("localhost", 8080).bind(route)
}

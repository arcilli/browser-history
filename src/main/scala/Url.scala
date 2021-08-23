import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import cats.effect.{ContextShift, IO}

import scala.concurrent.duration._
import java.sql.Timestamp
import scala.language.postfixOps
import spray.json._
import doobie._
import doobie.implicits._
import doobie.implicits.javasql._

import scala.concurrent.ExecutionContext

/*
    GET api/urls - all the urls from the database
    GET api/urls/X - the url with the given id
    GET api/urls?urlid=X
    POST api/urls - store the url given in the body
 */

case class Url(userId: Int, value: String, timestamp: Timestamp)

object DatabaseConnection {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:browser_history", "postgres", ""
  )

  def storeUrl(userId: Int, timestamp: Timestamp, value: String): Int = {
    sql"insert into urls(timestamp, value, userid) values ($timestamp, $value, $userId)"
      .update
      .run
      .transact(xa)
      .unsafeRunSync()
  }

  def getAllUrls: List[Url] = {
    sql"select userid, value, timestamp from urls".query[Url]
      .to[List]
      .transact(xa)
      .unsafeRunSync()
  }

  def getAllUrlsForUser(userId: Int): List[Url] = {
    sql"select userid, value, timestamp from urls where userid=$userId".query[Url]
      .to[List]
      .transact(xa)
      .unsafeRunSync()
  }

  def getUrlById(urlId: Int): Option[Url] = {
    sql"select userid, value, timestamp from urls where id=$urlId".query[Url]
      .option
      .transact(xa)
      .unsafeRunSync()

  }

  def getUrlByUserIdAndUrlId(urlId: Int): Option[Url] = {
    sql"select userid, value, timestamp from urls where id=$urlId".query[Url]
      .option
      .transact(xa)
      .unsafeRunSync()
  }
}

trait UrlJsonProtocol extends DefaultJsonProtocol {
  implicit object TimestampFormat extends JsonFormat[Timestamp] {
    override def write(obj: Timestamp): JsValue = JsString(obj.toString)

    override def read(json: JsValue): Timestamp =
      json match {
        case JsString(time) => Timestamp.valueOf(time)
        case _ => throw DeserializationException("Expected a Date as a String")
      }
  }

  implicit val urlFormat = jsonFormat3(Url)
}

object MainUrls extends App with UrlJsonProtocol with SprayJsonSupport {
  implicit val system = ActorSystem("Url")

  import DatabaseConnection._

  implicit val timeout = Timeout(2 seconds)

  val route =
    pathPrefix("api" / "urls") {
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

  Http().newServerAt("localhost", 8080).bind(route)
}

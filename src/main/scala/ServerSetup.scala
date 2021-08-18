import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{complete, pathEndOrSingleSlash}

object ServerSetup extends App{


  implicit val system = ActorSystem("Setup")

  val route =
    pathEndOrSingleSlash{
      complete(
        HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Hello world!
            | </body>
            |</html>
            |""".stripMargin
        )
      )
    }

  Http().newServerAt("localhost", 8080).bind(route)
}

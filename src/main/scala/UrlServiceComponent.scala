import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json._

trait UrlServiceComponent {
  this: UrlRepositoryComponent =>

  val urlService: UrlService

  class UrlService extends UrlJsonProtocol with SprayJsonSupport {
    val urlRoute: Route =
      path("urls") {
        post {
          entity(as[Url]) { url =>
            if (urlRepository.storeUrl(url.userId, url.timestamp, url.value).equals(1))
              complete(StatusCodes.OK)
            else
              complete(StatusCodes.InternalServerError)
          }
        } ~
          get {
            parameter(Symbol("urlid").as[Int]) { urlId =>
              urlRepository.getUrlById(urlId) match {
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
                val urls = urlRepository.getAllUrlsForUser(userId)
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
                    urlRepository.getAllUrls.toJson.prettyPrint
                  ))
              }
          }
      }
  }

}

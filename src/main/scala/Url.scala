import java.sql.Timestamp
import scala.language.postfixOps
import spray.json._

/*
    GET api/urls - all the urls from the database
    GET api/urls/X - the url with the given id
    GET api/urls?urlid=X
    POST api/urls - store the url given in the body
 */

case class Url(userId: Int, value: String, timestamp: Timestamp)

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

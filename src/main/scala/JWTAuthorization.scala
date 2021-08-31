import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtSprayJson}
import spray.json.{JsObject, JsString}

import java.util.concurrent.TimeUnit
import scala.util.{Failure, Success}


object JwtAuthorization extends SprayJsonSupport {
  val algorithm = JwtAlgorithm.HS256
  val notPassword = "browserhistory1"

  var tokensMap = scala.collection.mutable.Map[String, String]()


  def createToken(username: String, expirationPeriodInDays: Int): String = {
    val claims = JwtClaim(
      expiration = Some(System.currentTimeMillis() / 1000 + TimeUnit.DAYS.toSeconds(expirationPeriodInDays)),
      issuedAt = Some(System.currentTimeMillis() / 1000),
      issuer = Some("browserhistory"),
      content = JsObject("username" -> JsString(username)).prettyPrint
    )

    val tok = JwtSprayJson.encode(claims, notPassword, algorithm)
    tokensMap.put(username,tok)
    tok
  }

    def isTokenExpired(token: String): Boolean = JwtSprayJson.decode(token, notPassword, Seq(algorithm)) match {
      case Success(claims) => claims.expiration.getOrElse(0L) < System.currentTimeMillis() / 1000
      case Failure(_) => true
    }

    def isTokenValid(token: String): Boolean = {
      if(JwtSprayJson.isValid(token, notPassword, Seq(algorithm))){
        if(tokensMap.exists(_._2==token)){
          !isTokenExpired(token)
        }
        else return false
      }
      false
    }

}
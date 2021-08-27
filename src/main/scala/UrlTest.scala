import ServerSetup.{urlFormat, urlRoute}
import UrlRepositoryImplementation.{getAllUrls, getAllUrlsForUser}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.sql.Timestamp


class UrlTest extends AnyWordSpec
  with Matchers
  with ScalatestRouteTest
  with UserJsonProtocol
  with SprayJsonSupport {

  "A url" should {
    "Get all URLs from DB" in {
      Get("/urls") ~> urlRoute ~> check {
        status shouldBe StatusCodes.OK
        entityAs[List[Url]] shouldBe getAllUrls
      }
    }
    "Store an url visited by a user" in {
      val newUrl = Url(1, "facebook.com", new Timestamp(17842L))
      Post("/urls", newUrl) ~> urlRoute ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "Get all URLs for user" in {
      Get("/urls?userid=4") ~> urlRoute ~> check {
        status shouldBe StatusCodes.OK
        responseAs[List[Url]] shouldBe getAllUrlsForUser(4)
      }
    }

    "Get url with ID" in {
      Get("/urls?urlid=6") ~> urlRoute ~> check {
        status shouldBe StatusCodes.OK
        entityAs[Option[Url]] shouldBe Some(Url(2, "levi9.com", new Timestamp(1577865600000L)))
      }
    }
    "Get URL with non-existent ID" in {
      Get("/urls?urlid=1000") ~> urlRoute ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
    "Get all URLs for user with non-existent ID" in {
      Get("/urls?userid=1000") ~> urlRoute ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }
}
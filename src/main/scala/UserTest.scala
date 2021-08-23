import UserDatabaseImplementation.getAll
import UserRoute.registerAndLoginRoute
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class UserTest
  extends AnyWordSpec
    with Matchers
    with ScalatestRouteTest
    with UserJsonProtocol
    with SprayJsonSupport {

    "a user" should{
      "Register: insert a new user - username unique" in{
        val newUser = User("jinx", "jinx01")
        Post("/register", newUser) ~> registerAndLoginRoute ~> check{
          status shouldBe StatusCodes.OK
        }
      }
      "Register: insert a new user - username not unique" in{
        val newUser = User("Bing", "jinx01")
        Post("/register", newUser) ~> registerAndLoginRoute ~> check{
          status shouldBe StatusCodes.Forbidden
        }
      }
      "Get all users" in{
        Get("/users") ~> registerAndLoginRoute ~> check{
          status shouldBe StatusCodes.OK
          entityAs[List[UserRecord]] shouldBe getAll
        }
      }
      "Get a user by id" in{
        Get("/user/1") ~> registerAndLoginRoute ~> check{
          status shouldBe StatusCodes.OK
          responseAs[Option[UserRecord]] shouldBe Some(UserRecord(1, "bing", "123"))
        }
      }
      "Login user - success" in{
        val user = User("Soniq", "soniq")
        Get("/login", user) ~> registerAndLoginRoute ~> check{
          status shouldBe StatusCodes.OK
        }
      }
      "Login user - fail" in{
        val user = User("Soniq", "soni")
        Get("/login", user) ~> registerAndLoginRoute ~> check{
          status shouldBe StatusCodes.NotFound
        }
      }
      "Change username - success" in{
        val name1: String = "Soniq"
        val name2: String = "Soni"
        Patch(s"/changeName?name1=$name1&name2=$name2") ~> registerAndLoginRoute ~> check{
          status shouldBe StatusCodes.OK
        }
      }
      "Change username - fail" in{
        val name1: String = "Soniq"
        val name2: String = "bing"
        Patch(s"/changeName?name1=$name1&name2=$name2") ~> registerAndLoginRoute ~> check{
          status shouldBe StatusCodes.Forbidden
        }
      }
    }

}

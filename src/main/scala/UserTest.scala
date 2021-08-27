import ServerSetup.registerAndLoginRoute
import UserRepositoryImplementation.getAll
import UserRoute.registerHashPass
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class UserTest extends AnyWordSpec with ScalatestRouteTest with Matchers with MockitoSugar with UserJsonProtocol with SprayJsonSupport {
  val userRepositoryMock = mock[UserRepository]

  val registerValid = User("Gabriel", "glamour")
  val registerInvalid = User("Gabriel", "glamorous") // invalid because of the unique username constraint
  val unregisteredUser = User("Roxana", "snow00")
  val foundUser = Some(User("Gabriel", "glamour"))
  val anotherRegisterUser = User("Ramona", "summer12")

  when(userRepositoryMock.insertUser(registerValid.username, registerHashPass(registerValid.password))).thenReturn(1)
  // when(userRepositoryMock.insertUser(registerInvalid.username, registerHashPass(registerInvalid.password))).thenReturn(0)
  //
  //  when(userRepositoryMock.findUserByName(registerValid.username)).thenReturn(foundUser)
  //
  //  when(userRepositoryMock.insertUser(anotherRegisterUser.username, anotherRegisterUser.password)).thenReturn(1)
  //
  //  when(userRepositoryMock.getAll).thenReturn(List(
  //    UserRecord(1, "Gabriel", registerHashPass("glamour")),
  //    UserRecord(2, "Ramona", registerHashPass("summer12")))
  //  )
  //
  //  when(userRepositoryMock.updateUsername("Gabriel", "Ovidiu")).thenReturn(1)



  "a user" should {
    "Register: insert a new user - username unique" in {
      Post("/register", registerValid) ~> registerAndLoginRoute ~> check {
        status shouldBe StatusCodes.OK
      }
    }
    "Register: insert a new user - username not unique" in {
      Post("/register", registerInvalid) ~> registerAndLoginRoute ~> check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
    "Get all users" in {
      Get("/users") ~> registerAndLoginRoute ~> check {
        status shouldBe StatusCodes.OK
        entityAs[List[UserRecord]] shouldBe getAll
      }
    }
    "Get a user by id" in {
      Get("/user/1") ~> registerAndLoginRoute ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Option[UserRecord]] shouldBe Some(UserRecord(1, "bing", "123"))
      }
    }
    "Login user - success" in {
      Get("/login", registerValid) ~> registerAndLoginRoute ~> check {
        status shouldBe StatusCodes.OK
      }
    }
    "Login user - fail due to wrong password" in {
      Get("/login", registerInvalid) ~> registerAndLoginRoute ~> check {
        status shouldBe StatusCodes.Forbidden
      }
    }
    "Login user - fail due to unregistered user" in {
      Get("/login", unregisteredUser) ~> registerAndLoginRoute ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
    "Change username - success" in {
      val name1: String = registerValid.username
      val name2: String = "Gabriela"
      Patch(s"/changeName?name1=$name1&name2=$name2") ~> registerAndLoginRoute ~> check {
        status shouldBe StatusCodes.OK
      }
    }
    "Change username - fail due to unique username constraint" in {
      val name1: String = registerValid.username
      val name2: String = anotherRegisterUser.username
      Patch(s"/changeName?name1=$name1&name2=$name2") ~> registerAndLoginRoute ~> check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
    "Change username - fail due to unregistered user" in {
      val name1: String = unregisteredUser.username
      val name2: String = "Roxi"
      Patch(s"/changeName?name1=$name1&name2=$name2") ~> registerAndLoginRoute ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }

  // verify(userRepositoryMock).insertUser(registerValid.username, registerHashPass(registerValid.password))
}

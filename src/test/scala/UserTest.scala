import UserRoute.registerHashPass
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class UserTest extends AnyWordSpec
  with ScalatestRouteTest
  with Matchers
  with MockitoSugar
  with UserJsonProtocol
  with SprayJsonSupport
  with UserServiceComponent
  with UserRepositoryComponent {

  override val userService = new UserService
  override val userRepository = mock[UserRepositoryImplementation]

  val registerValid = User("Gabriel", "glamour")
  val registerInvalid = User("Gabriel", "glamorous") // invalid because of the unique username constraint
  val unregisteredUser = User("Roxana", "snow00")
  val foundUser = Some(User("Gabriel", "108486107"))
  val foundUserById = Some(UserRecord(1, "Gabriel", "108486107"))
  val anotherRegisterUser = User("Ramona", "summer12")

  when(userRepository.insertUser(registerValid.username, registerHashPass(registerValid.password))).thenReturn(1)
  when(userRepository.insertUser(registerInvalid.username, registerHashPass(registerInvalid.password))).thenReturn(0)

  when(userRepository.getAll).thenReturn(List(
    UserRecord(1, "Gabriel", registerHashPass("glamour")),
    UserRecord(2, "Ramona", registerHashPass("summer12")))
  )

  when(userRepository.findUserById(1)).thenReturn(foundUserById)
  when(userRepository.findUserById(10)).thenReturn(None)


  when(userRepository.findUserByName(registerValid.username)).thenReturn(foundUser)
  when(userRepository.findUserByName(registerInvalid.username)).thenReturn(foundUser)
  when(userRepository.findUserByName(unregisteredUser.username)).thenReturn(None)

  when(userRepository.updateUsername(registerValid.username, "Gabriela")).thenReturn(1)
  when(userRepository.updateUsername(registerValid.username, anotherRegisterUser.username)).thenReturn(0)
  when(userRepository.updateUsername(unregisteredUser.username, "Roxi")).thenReturn(1)

  "a user" should {
    "Register: insert a new user - username unique" in {
      Post("/register", registerValid) ~> userService.registerAndLoginRoute ~> check {
        status shouldBe StatusCodes.OK
      }
    }
    "Register: insert a new user - username not unique" in {
      Post("/register", registerInvalid) ~> userService.registerAndLoginRoute ~> check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
    "Get all users" in {
      Get("/users") ~> userService.registerAndLoginRoute ~> check {
        status shouldBe StatusCodes.OK
        entityAs[List[UserRecord]] shouldBe userRepository.getAll
      }
    }
    "Get a user by id" in {
      Get("/user/1") ~> userService.registerAndLoginRoute ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Option[UserRecord]] shouldBe Some(UserRecord(1, "Gabriel", "108486107"))
      }
    }
    "Get a user by id - fail" in {
      Get("/user/10") ~> userService.registerAndLoginRoute ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
    "Login user - success" in {
      Get("/login", registerValid) ~> userService.registerAndLoginRoute ~> check {
        status shouldBe StatusCodes.OK
      }
    }
    "Login user - fail due to wrong password" in {
      Get("/login", registerInvalid) ~> userService.registerAndLoginRoute ~> check {
        status shouldBe StatusCodes.Forbidden
      }
    }
    "Login user - fail due to unregistered user" in {
      Get("/login", unregisteredUser) ~> userService.registerAndLoginRoute ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
    "Change username - success" in {
      val name1: String = registerValid.username
      val name2: String = "Gabriela"
      Patch(s"/changeName?name1=$name1&name2=$name2") ~> userService.registerAndLoginRoute ~> check {
        status shouldBe StatusCodes.OK
      }
    }
    "Change username - fail due to unique username constraint" in {
      val name1: String = registerValid.username
      val name2: String = anotherRegisterUser.username
      Patch(s"/changeName?name1=$name1&name2=$name2") ~> userService.registerAndLoginRoute ~> check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
    "Change username - fail due to unregistered user" in {
      val name1: String = unregisteredUser.username
      val name2: String = "Roxi"
      Patch(s"/changeName?name1=$name1&name2=$name2") ~> userService.registerAndLoginRoute ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }

}

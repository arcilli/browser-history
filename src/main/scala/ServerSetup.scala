import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route


trait SetupActor {
  implicit val system = ActorSystem("SetupActor")
}

trait ServerSetup extends UserServiceComponent
  with UserRepositoryComponent
  with UrlServiceComponent
  with UrlRepositoryComponent {
  val database: DataBaseConn
}

object Main extends App with SetupActor with ServerSetup {

  override val database = DataBaseConn

  override val userService: UserService = new UserService
  override val userRepository: UserRepositoryImplementation = new UserRepositoryImplementation(database)

  override val urlService = new UrlService
  override val urlRepository = new UrlRepositoryImplementation(database)

  val route: Route =
    pathPrefix("api") {
      userService.registerAndLoginRoute ~
        urlService.urlRoute

    }

  Http().newServerAt("localhost", 8080).bind(route)

}

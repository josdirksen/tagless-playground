package tagless.services

import org.http4s.server.Server
import cats.MonadError
import cats.effect.kernel.MonadCancel
import org.http4s.blaze.server.BlazeServerBuilder
import scala.concurrent.ExecutionContext
import cats.effect.kernel.Async
import cats.syntax.functor._, cats.syntax.flatMap._
import org.http4s.HttpRoutes
import cats.Monad
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import cats.effect.kernel.Resource
import org.http4s.client.Client
import org.http4s.blaze.client.BlazeClientBuilder

object http4sClient {

  /**
   * Simple DSL to get an instance of a client
   */
  trait Http4sClientDSL[F[_]] {
    val client: Resource[F, Client[F]]
  }

  object Http4sClientDSL {
    def apply[F[_]](implicit F: Http4sClientDSL[F]): F.type = F
  }

  /**
   * Live version
   */
  case class LiveHttp4sClientInterpreter[F[_]: Async]()(implicit ec: ExecutionContext) extends Http4sClientDSL[F] {
    override val client: Resource[F, Client[F]] = BlazeClientBuilder[F](ec).resource
  }

}

object http4sServer {

  /**
   * This trait provides access to a Http4sServer dependency. It is
   * wrapped in the F monad, so we can signal errors during setup.
   *
   * Wondering whether these should be called DSL, since these are
   * just the low level dependencies.
   */
  trait Http4sServerDSL[F[_]] {
    val server: Resource[F, Server]
  }

  /**
   * Helper object to access the instance of the DSL that is in scope
   */
  object Http4ServerDSL {
    def apply[F[_]](implicit F: Http4sServerDSL[F]): F.type = F
  }

  case class routes[F[_]: Monad]() {

    private val dsl = Http4sDsl[F]
    import dsl._

    val routes: HttpRoutes[F] = HttpRoutes
      .of[F] {
        case GET -> Root / "users" / IntVar(id) => {
          Created("A value here")
        }
        case POST -> Root / "users" => {
          Created("And another one here")
        }
      }
  }

  /**
   * Live instance of the HttpsServer service.
   *
   * @param ec
   */
  case class LiveHttp4sServerInterpreter[
      F[_]: MonadCancel[*[_], Throwable]: Async: ConfigDSL
  ]()(implicit ec: ExecutionContext)
      extends Http4sServerDSL[F] {

    override val server: Resource[F, Server] =
      for {
        config <- Resource.eval(ConfigDSL[F].config)
        server <- BlazeServerBuilder[F](ec)
          .bindHttp(config.apiConfig.port, config.apiConfig.endpoint)
          .withHttpApp(routes[F]().routes.orNotFound)
          .resource
      } yield (
        server
      )
  }
}

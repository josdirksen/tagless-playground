package tagless

import tagless.services.LiveConfigInterpreter
import cats.effect.IO
import tagless.services.http4sServer.LiveHttp4sServerInterpreter
import scala.concurrent.ExecutionContext
import cats.effect.ExitCode
import cats.effect.IOApp

object Runner extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    // get all the services, and add them to the implicit scope
    implicit val ec = ExecutionContext.global

    // the concrete implementations mapped to the IO monad
    implicit val configService = LiveConfigInterpreter[IO]()
    implicit val http4sClient = services.http4sClient.LiveHttp4sClientInterpreter[IO]()
    val http4s = LiveHttp4sServerInterpreter[IO]()

    implicit val tempClient = services.tempClient.LiveTempClientInterpreter[IO]()

    val program = for {
      config <- configService.config
      stream <- tempClient.temperatureStream
      _ <- stream
        .evalTap({ t =>
          println(t)
          IO.unit
        })
        .compile
        .drain
        .start
      server <- http4s.server.use(_ => IO.never).as(ExitCode.Success)
    } yield {
      server
    }

    program
  }

}

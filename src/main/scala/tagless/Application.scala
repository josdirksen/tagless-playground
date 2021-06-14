package tagless

import tagless.services.LiveConfigInterpreter
import cats.effect.IO
import tagless.services.http4sServer.LiveHttp4sServerInterpreter
import scala.concurrent.ExecutionContext
import cats.effect.ExitCode
import cats.effect.IOApp
import tagless.services.repo.storage
import tagless.services.db.mongo

/**
 * Simple runner which uses the CATS IO monad to run everything
 */
object Runner extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    // get all the services, and add them to the implicit scope
    implicit val ec = ExecutionContext.global

    // the concrete implementations mapped to the IO monad
    implicit val configService = LiveConfigInterpreter[IO]()
    implicit val http4sClient = services.http4sClient.LiveHttp4sClientInterpreter[IO]()
    implicit val MongoDBConnectionDSL = mongo.LiveMongoDBConnectionInterpreter[IO]()
    val http4s = LiveHttp4sServerInterpreter[IO]()

    implicit val tempClient = services.tempClient.LiveTempClientInterpreter[IO]()
    val tempStorage = storage.TemperatureStorageInterpreter[IO]()

    val program = for {
      config <- configService.config
      // consume the stream
      stream <- tempClient.temperatureStream
      _ <- stream
        .evalMap(tempStorage.insert)
        .compile
        .drain
        .start
      // start the server and keep running
      server <- http4s.server.use(_ => IO.never).as(ExitCode.Success)
    } yield {
      server
    }

    // return the program to execute
    program
  }

}

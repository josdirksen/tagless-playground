package tagless.services.repo

import tagless.model.Temperature

object storage {
  trait TemperatureStorageDSL[F[_]] {
    def insert(temperature: Temperature): F[Unit]
    def getAll(): F[List[Temperature]]
  }

  case class TempClientInterpreter[F[_]: ConfigDSL]() extends TemperatureStorageDSL[F] {

    override def insert(temperature: Temperature): F[Unit] = ???

    override def getAll(): F[List[Temperature]] = ???

  }
}

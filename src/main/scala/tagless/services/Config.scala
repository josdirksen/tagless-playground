package tagless.services

import scala.concurrent.duration.FiniteDuration
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import cats.MonadError
import cats.syntax.monadError._
import cats.implicits._

case class Config(apiConfig: ApiConfig, temperatureConfig: TemperatureConfig, dbConfig: DBConfig)
case class ApiConfig(endpoint: String, port: Int)
case class TemperatureConfig(endpoint: String, apiKey: String, interval: FiniteDuration)
case class DBConfig(endpoint: String)

/**
 * Basic DSL trait is type, constraint agnostic.
 */
trait ConfigDSL[F[_]] {
  val config: F[Config]
}

object ConfigDSL {
  def apply[F[_]](implicit F: ConfigDSL[F]): F.type = F
}

/**
 * A pureconfig based interpreter. Note that we use the kind projector, so we can
 * correctly specify the MonadError constraint. If we don't use this we have to
 * either add it as a desugared implicit, or create a custom type.
 */
case class LiveConfigInterpreter[F[_]: MonadError[*[_], Throwable]]() extends ConfigDSL[F] {

  override val config: F[Config] = {

    /**
     * We can wrap it in an either, if we do that then we don't
     * really need any other typeclasses, we can also say that
     * we expect the result to be mapped to a monadError. That
     * means that the F[_] used, should satisfy the MonadError
     * constraint.
     */
    val pp = ConfigSource.default
      .load[Config]
      .left
      .map(pureconfig.error.ConfigReaderException.apply)

    // summon the monadError instance and convert to an either
    MonadError[F, Throwable].fromEither(pp)
  }
}

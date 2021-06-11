package tagless.services

import tagless.model.Temperature
import fs2.Stream
import cats.effect.Temporal
import scala.concurrent.duration._
import cats.syntax.functor._, cats.syntax.flatMap._
import http4sClient.Http4sClientDSL
import cats.MonadError

object tempClient {

  trait TempClientDSL[F[_]] {
    val temperatureStream: F[Stream[F, Temperature]]
  }

  case class LiveTempClientInterpreter[F[_]: MonadError[*[_], Throwable]: Temporal: ConfigDSL: Http4sClientDSL]()
      extends TempClientDSL[F] {

    import org.http4s.circe._
    import io.circe.generic.auto._
    import TempClientLive.OpenWeather._

    implicit val userDecoder = jsonOf[F, OWResult]

    /**
     * Get a stream in an effectful way.
     */
    override val temperatureStream: F[Stream[F, Temperature]] = for {
      config <- ConfigDSL[F].config
      stream = (Stream(1) ++ Stream.awakeEvery(config.temperatureConfig.interval)).evalMap { _ =>
        makeTemperatureCall(config.temperatureConfig.endpoint + config.temperatureConfig.apiKey)
      }
    } yield {
      stream
    }

    /**
     * Make an actual call to a rest endpoint
     */
    private def makeTemperatureCall(url: String)(implicit clientDSL: Http4sClientDSL[F]): F[Temperature] =
      clientDSL.client.use {
        _.expect[TempClientLive.OpenWeather.OWResult](url).map(t => Temperature(t.dt, t.main.temp))
      }

  }

  object TempClientLive {

    object OpenWeather {
      // the openweather model
      case class OWResult(coord: OWCoord, main: OWMain, visibility: Integer, wind: OWWind, dt: Long)
      case class OWCoord(lat: Double, lon: Double)
      case class OWMain(
          temp: Double,
          feels_like: Double,
          temp_min: Double,
          temp_max: Double,
          pressure: Int,
          humidity: Int
      )
      case class OWWind(speed: Double, deg: Long, gust: Double)
    }
  }

}

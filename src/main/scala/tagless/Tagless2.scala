// package tagless

// import model._
// import cats.{Monad, MonadError}
// import cats.syntax.functor._, cats.syntax.flatMap._
// import cats.syntax.either._
// import cats.Applicative
// import cats.syntax.monadError._
// import cats.implicits.catsStdInstancesForEither

// object model {

//   sealed trait DomainError {
//     case class SomeError() extends DomainError
//   }

//   case class Temperature(val t: Double)
//   case class Humidity(val t: Double)

//   type Result[A] = Either[DomainError, A]
// }

// trait TemperatureDSL[F[_]] {
//   def getTemperature(): F[Result[Temperature]]
//   def updateTemperature(location: String): F[Result[Temperature]]
// }

// trait HumidityDSL[F[_]] {
//   def getHumidity(): F[Either[DomainError, Humidity]]
// }

// object WeatherService {

//   /** Get both using an applicative to map the Tuple of eithers
//     * to an either of a tuple.
//     */
//   def getBothMonad[F[_]: Monad](implicit
//       tempDsl: TemperatureDSL[F],
//       humDsl: HumidityDSL[F]
//   ): F[Result[(Temperature, Humidity)]] = for {
//     t <- tempDsl.getTemperature()
//     h <- humDsl.getHumidity()
//   } yield {
//     val n = Applicative[Result]
//     n.tuple2(t, h)
//   }

//   /** A bit weird, since Either provides an instance of MonadError
//     */
//   type RMonadError[F[_]] = MonadError[F, DomainError]
//   def getBothMonadError[F[_]: RMonadError](implicit
//       tempDsl: TemperatureDSL[F],
//       humDsl: HumidityDSL[F]
//   ): F[(Temperature, Humidity)] = for {
//     t <- tempDsl.getTemperature()
//     h <- humDsl.getHumidity()
//   } yield {
//     (MonadError[F, DomainError].fromEither(t), MonadError[F, DomainError].fromEither(h))
//   }
// }

// object Runner extends App {

//   println("running")

// }

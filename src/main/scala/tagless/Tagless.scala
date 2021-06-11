// package tagless

// import model._
// import cats.{Monad, MonadError}
// import cats.syntax.functor._, cats.syntax.flatMap._
// import cats.syntax.either._
// import cats.Applicative
// import cats.syntax.monadError._
// import cats.implicits.catsStdInstancesForEither
// import cats.data.EitherT
// import cats.effect.kernel.Sync
// import cats.effect.kernel.Async
// import cats.effect.kernel.AsyncPlatform
// import cats.effect.kernel.syntax.async
// import cats.effect.IO
// import cats.effect.kernel.Clock
// import scala.concurrent.duration.Duration

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
// }

// trait HumidityDSL[F[_]] {
//   def getHumidity(): F[Result[Humidity]]
// }

// /** This would be a generic implementation, where we model the retrieval of the humitidy
//   * using the sync monad. So any IO instance that provides a mapping to Sync can be used here.
//   */
// class LiveHumidityDLS[F[_]: Sync] extends HumidityDSL[F] {

//   override def getHumidity(): F[Result[Humidity]] = Sync[F].delay {
//     Either.right(Humidity(1))
//   }
// }

// class LiveTemperatureDSL[F[_]: Async: Sync: Clock] extends TemperatureDSL[F] {

//   val ev = Async[F]

//   override def getTemperature(): F[Result[Temperature]] = {
//     ev.async[Result[Temperature]] { cb =>
//       val sleep = Clock[F].sleep(Duration(1, "seconds"))

//       sleep.flatMap { _ =>
//         ev.delay {
//           // call to resolve the callback
//           println("Some text here")
//           cb(Either.right(Either.right(Temperature(12))))

//           // regiser an effect which is called when the fiber this async
//           // thingy is running in is cancelled.
//           // we need to return an Option[F[Unit]]. That is called when the fiber is
//           // cancelled
//           Some(ev.delay { println("Doing some cancellation") })
//         }
//       }
//     }
//   }
// }

// object WeatherService {

//   /** Get both, since we want to fail when one of the either fails
//     * we have to use the EitherT
//     */
//   def getBothMonad[F[_]: Monad](implicit
//       tempDsl: TemperatureDSL[F],
//       humDsl: HumidityDSL[F]
//   ): F[Result[(Temperature, Humidity)]] = {
//     val result = for {
//       // we use EitherT here, so we don't have to worry about the outer
//       // monad and can work with the inner Either.
//       t <- EitherT(tempDsl.getTemperature())
//       h <- EitherT(humDsl.getHumidity())
//     } yield {
//       (t, h)
//     }

//     // convert the EitherT[F, A, B] back to a F[Either[A, B]]
//     result.value
//   }

//   /** We could lift the failing either to a monadError. This would allow
//     * us to not worry about the chosen monadError implementation, as long
//     * as we have a MonadError one
//     */
//   type RMonadError[F[_]] = MonadError[F, DomainError]
//   def getBothMonadAsError[F[_]: RMonadError](implicit
//       tempDsl: TemperatureDSL[F],
//       humDsl: HumidityDSL[F]
//   ): F[(Temperature, Humidity)] = {

//     import cats.syntax.MonadErrorSyntax

//     val ev = implicitly[RMonadError[F]]
//     val result = for {
//       // we can also use the rethrow, which maps the F[Either[A,B]] to a
//       // MonadError if that one is in scope.
//       t <- tempDsl.getTemperature().rethrow
//       h <- humDsl.getHumidity().rethrow
//     } yield {
//       (t, h)
//     }

//     result
//   }
// }

// // https://blog.softwaremill.com/final-tagless-seen-alive-79a8d884691d
// // Do something like that
// class WeatherServiceSync[F[_]: Sync]() {

//   def getBothMonad[F[_]: Monad](implicit
//       tempDsl: TemperatureDSL[F],
//       humDsl: HumidityDSL[F]
//   ): F[Result[(Temperature, Humidity)]] = {
//     val result = for {
//       // we use EitherT here, so we don't have to worry about the outer
//       // monad and can work with the inner Either.
//       t <- EitherT(tempDsl.getTemperature())
//       h <- EitherT(humDsl.getHumidity())
//     } yield {
//       (t, h)
//     }

//     // convert the EitherT[F, A, B] back to a F[Either[A, B]]
//     result.value
//   }
// }

// object Runner extends App {
//   implicit val runtime = cats.effect.unsafe.IORuntime.global

//   val p = new LiveTemperatureDSL[IO]()

//   val pp = p.getTemperature
//   pp.unsafeRunSync()
//   println("blaat")
// }

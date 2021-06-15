package tagless.services.repo

import tagless.model.Temperature
import tagless.services.ConfigDSL
import tagless.services.db.mongo.MongoDBConnectionDSL
import cats.MonadError
import cats.syntax.functor._, cats.syntax.flatMap._
import cats.syntax.monadError._
import scala.reflect.ClassTag

import org.mongodb.scala._
import org.mongodb.scala.bson.codecs.DocumentCodecProvider
import org.mongodb.scala.bson.codecs.Macros
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{fromRegistries, fromProviders}
import scala.util.Try
import cats.effect.kernel.MonadCancel

import fs2.interop.reactivestreams._
import cats.effect.kernel.Async
import org.slf4j.LoggerFactory

object storage {
  trait TemperatureStorageDSL[F[_]] {
    def insert(temperature: Temperature): F[Unit]
    def getAll(): F[List[Temperature]]
  }

  object TemperatureStorageDSL {
    def apply[F[_]](implicit F: TemperatureStorageDSL[F]): F.type = F
  }

  case class TemperatureStorageInterpreter[
      F[_]: MonadCancel[*[_], Throwable]: Async: ConfigDSL: MongoDBConnectionDSL
  ]() extends TemperatureStorageDSL[F] {

    val temperatureCodecProvider = Macros.createCodecProvider[Temperature]()
    val codecRegistry = fromRegistries(fromProviders(temperatureCodecProvider), DEFAULT_CODEC_REGISTRY)
    val ME = MonadError[F, Throwable]

    override def insert(temperature: Temperature): F[Unit] =
      withCollection[Unit, Temperature] { c =>
        c.insertOne(temperature)
          .toStream[F]
          .compile
          .toList
          .map(_.length)
          // naively assume that when we get 1 insert result everything is fine
          .flatMap {
            case 1 => ME.pure()
            case _ => ME.raiseError[Unit](new IllegalArgumentException("Expected result from mongodb"))
          }
      }

    override def getAll(): F[List[Temperature]] = {
      withCollection[List[Temperature], Temperature] {
        _.find()
          .toStream[F]
          .compile
          .toList
      }
    }

    /**
     * Get the database and collection to which to store.
     *
     * @param f function to call within the context of this collection
     * @return result of wrapped
     */
    private def withCollection[A, T: ClassTag](
        f: MongoCollection[T] => F[A]
    ): F[A] = MongoDBConnectionDSL[F].mongoClient.use[A] { mongoClient =>
      val collection =
        mongoClient
          .getDatabase("sampleservice")
          .withCodecRegistry(codecRegistry)
          .getCollection[T]("temperatures")

      f(collection)
    }
  }

}

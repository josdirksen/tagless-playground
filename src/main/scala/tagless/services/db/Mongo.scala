package tagless.services.db

import org.mongodb.scala.MongoClient
import cats.MonadError
import tagless.services.ConfigDSL
import cats.syntax.functor._, cats.syntax.flatMap._
import scala.util.Try
import cats.effect.kernel.Resource
import org.slf4j.LoggerFactory
import cats.effect.kernel.Sync
import cats.syntax.monadError._

object mongo {

  trait MongoDBConnectionDSL[F[_]] {
    val mongoClient: Resource[F, MongoClient]
  }

  object MongoDBConnectionDSL {
    def apply[F[_]](implicit F: MongoDBConnectionDSL[F]): F.type = F
  }

  case class LiveMongoDBConnectionInterpreter[F[_]: Sync: ConfigDSL]() extends MongoDBConnectionDSL[F] {

    val logger = LoggerFactory.getLogger("test")

    val ME = MonadError[F, Throwable]
    val Config = ConfigDSL[F]

    override val mongoClient = Resource.make(acquireConnection)(releaseConnection)

    /**
     * Acquire a connection
     */
    private def acquireConnection()(implicit config: ConfigDSL[F]) =
      for {
        c <- config.config
        r <- Sync[F].blocking(MongoClient(c.dbConfig.endpoint))
      } yield {
        r
      }

    /**
     * Release the mongo client, wrap in sync since it's effectful
     */
    private def releaseConnection(mongoClient: MongoClient) = Sync[F].blocking(mongoClient.close)

  }
}

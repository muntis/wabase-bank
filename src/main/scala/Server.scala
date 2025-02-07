package uniso.app

import org.apache.pekko.event.Logging
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.directives.DebuggingDirectives
import org.wabase.{Loggable, config}

import scala.util._
import scala.concurrent.duration._


object AppServer extends scala.App with Loggable {

  import AppService._

  val port = scala.util.Try(config.getInt("port")).toOption.getOrElse(8090)

  val shutdownDeadline: FiniteDuration = 15.seconds

  Http()
    .newServerAt("0.0.0.0", port).bind(
      DebuggingDirectives.logRequestResult("Bank server", Logging.InfoLevel)(route)
    )
    .map(_.addToCoordinatedShutdown(shutdownDeadline)) // ← that simple!
    .foreach { server =>
      logger.info(s"HTTP service listening on: ${server.localAddress}")

      server.whenTerminationSignalIssued.onComplete { _ =>
        logger.info("Shutdown of HTTP service initiated")
      }

      server.whenTerminated.onComplete {
        case Success(_) => logger.info("Shutdown of HTTP endpoint completed")
        case Failure(_) => logger.error("Shutdown of HTTP endpoint failed")
      }
    }
}

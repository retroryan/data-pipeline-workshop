package stockActors

import akka.actor._
import akka.util.Timeout

import scala.concurrent.duration.{Duration, MILLISECONDS}

object Settings extends ExtensionKey[Settings]

class Settings(system: ExtendedActorSystem) extends Extension {

  private val config = system.settings.config

  implicit val askTimeout: Timeout =
    Duration(config.getDuration("ask-timeout", MILLISECONDS), MILLISECONDS)

  val sentimentUrl = config.getString("sentiment.url")

  val defaultTweetUrl = config.getString("tweet.url")

  val stockManagerActor: ActorRef =  system.actorOf(StockManagerActor.props())

  val dataWriterActor: ActorRef = system.actorOf(DataWriterActor.props())

  val stockSummaryActor: ActorRef = system.actorOf(StockSummaryActor.props())

}

trait SettingsActor {
  this: Actor =>

  val settings: Settings =
    Settings(context.system)
}

package actors

import play.api._
import play.api.libs.concurrent.Akka

/**
 * Lookup for actors used by the web front end.
 */
object Actors {

    private def actors(implicit app: Application) = app.plugin[Actors]
        .getOrElse(sys.error("Actors plugin not registered"))

    /**
     * Get the tweet loader client.
     */
    // def tweetLoader(implicit app: Application) = actors.tweetLoader
}

/**
 * Manages the creation of actors in the web front end.
 *
 * This is discovered by Play in the `play.plugins` file.
 */
class Actors(app: Application) extends Plugin {

    private def system = Akka.system(app)

    override def onStart() = {

    }

   //  private lazy val tweetLoader = system.actorOf(TweetLoader.props, "tweetLoader")
}

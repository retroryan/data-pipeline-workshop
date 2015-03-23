import play.api.libs.json.JsValue
import play.api.test._
import play.api.test.Helpers._
import org.scalatest.WordSpec
import org.scalatest.MustMatchers

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class ApplicationSpec extends WordSpec with MustMatchers {

  "Application" should {

    "send 404 on a bad request" in new WithApplication{
      route(FakeRequest(GET, "/boum")) must be(None)
    }

    "render the index page" in new WithApplication{
      val home = route(FakeRequest(GET, "/")).get

      status(home) must be(OK)
      contentType(home) must be(Some("text/html"))
      contentAsString(home) must include ("Hello Play Framework")
    }
  }
}

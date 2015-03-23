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
class IntegrationSpec extends WordSpec with MustMatchers {

  "Application" should {

    "work from within a browser" in new WithBrowser {

      browser.goTo("http://localhost:" + port)
      browser.pageSource must include ("Hello Play Framework")
    }
  }
}

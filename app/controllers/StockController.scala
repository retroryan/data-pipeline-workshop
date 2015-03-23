package controllers

import play.api.Play.current
import play.api.libs.json.JsValue
import play.api.mvc.{Action, Controller, WebSocket}
import stockActors.StockUserActor

object StockController extends Controller {

    def index = Action {
        Ok(views.html.stockIndex())
    }

    def ws = WebSocket.acceptWithActor[JsValue, JsValue] { implicit request => out =>
        StockUserActor.props(out)
    }

}

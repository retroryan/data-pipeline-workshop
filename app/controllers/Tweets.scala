package controllers

import play.api.mvc.{Action, Controller}

object Tweets extends Controller {

  def index = Action {
    Ok(views.html.index("Hello Play Framework"))
  }
}
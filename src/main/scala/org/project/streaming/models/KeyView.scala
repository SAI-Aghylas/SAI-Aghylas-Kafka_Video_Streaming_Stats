package org.project.streaming.models
import play.api.libs.json.{Json, OFormat}

case class KeyView( id: Int, category: String, title: String)
object KeyView {
  implicit val format :OFormat[KeyView]=Json.format[KeyView]
}

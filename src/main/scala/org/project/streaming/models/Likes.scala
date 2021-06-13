package org.project.streaming.models

import play.api.libs.json.{Json, OFormat}

case class Likes(_id: Int, score : Double){
  def EvalBestFilms (_id: Int, score: Double):  Unit ={
    if (score>4.0) return _id
  }
  def EvalWorstFilms (_id: Int, score: Double):  Unit ={
    if (score<2.0) return _id
  }
}
object Likes{
  implicit val format:OFormat[Likes]=Json.format[Likes]
}

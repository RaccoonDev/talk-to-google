package payperclick.engine.http.entities

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DeserializationException, JsArray, JsBoolean, JsNull, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

object SubmissionEntities {

  sealed trait Status
  object Active extends Status
  object Paused extends Status
  object Deactivated extends Status

  final case class Submission(id: String, sourceBigQueryTable: String, status: Status)
  final case class Submissions(submissions: Seq[Submission])

}

trait JsonSupport extends SprayJsonSupport {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import spray.json.DefaultJsonProtocol._
  import SubmissionEntities._

  implicit object StatusFormat extends RootJsonFormat[Status] {
    override def read(json: JsValue): Status = json match {
      case JsString("Active") => Active
      case JsString("Deactivated") => Deactivated
      case JsString("Paused") => Paused
      case _ =>  throw DeserializationException("Status unexpected")
    }

    override def write(obj: Status): JsValue = obj match {
      case Active => JsString("Active")
      case Deactivated => JsString("Deactivated")
      case Paused => JsString("Paused")
    }
  }

  implicit val submissionFormat: RootJsonFormat[Submission] = jsonFormat3(Submission)
  implicit val submissionsFormat: RootJsonFormat[Submissions] = jsonFormat1(Submissions)

}
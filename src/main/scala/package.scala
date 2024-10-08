package uniso

import akka.actor.ActorSystem
import org.wabase.currentTime

import scala.collection.immutable.{LazyList, Seq}
import scala.jdk.CollectionConverters._

package object app {
  val execution = new org.wabase.ExecutionImpl()(ActorSystem("bank-app-server"))

  def now = new java.sql.Timestamp(currentTime)
  def nowDate = new java.sql.Date(currentTime)

  def mapToJavaMap(map: Map[String, ?]): java.util.Map[String, AnyRef] = {
    val result = map.map { (entry: (String, ?)) =>
      (entry._1,
        entry._2 match {
          case l: List[_] => listToJavaList(l)
          case m: Map[String@unchecked, _] => mapToJavaMap(m)
          case r => r
        }
      )
    }
    result.asInstanceOf[Map[String, AnyRef]].asJava
  }

  def listToJavaList(list: List[?]): java.util.List[?] = {
    val result = list.toList.map {
      case l: List[_] => listToJavaList(l)
      case m: Map[String@unchecked, _] => mapToJavaMap(m)
      case r => r
    }
    result.asJava
  }

  def javaMapToMap(map: java.util.Map[String, ?]): Map[String, ?] = {
    val result = map.asScala.map(entry =>
      (entry._1,
        entry._2 match {
          case l: java.util.List[_] => javaListToList(l)
          case m: java.util.Map[String@unchecked, _] => javaMapToMap(m)
          case r => r
        }
      )
    ).toMap
    result.asInstanceOf[Map[String, ?]]
  }

  def javaListToList(list: java.util.List[?]): List[?] = {
    val result = list.asScala.toList.map {
      case l: java.util.List[_] => javaListToList(l)
      case m: java.util.Map[String@unchecked, _] => javaMapToMap(m)
      case r => r
    }
    result
  }

}

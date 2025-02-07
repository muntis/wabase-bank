package uniso

import org.apache.pekko.actor.ActorSystem
import org.wabase.currentTime

import scala.collection.immutable.{LazyList, Seq}
import scala.jdk.CollectionConverters._

package object app {
  val execution = new org.wabase.ExecutionImpl()(ActorSystem("bank-app-server"))

  def now = new java.sql.Timestamp(currentTime)
  def nowDate = new java.sql.Date(currentTime)

}

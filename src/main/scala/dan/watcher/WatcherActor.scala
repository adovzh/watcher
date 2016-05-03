package dan.watcher

import java.nio.file.Path

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive

import scala.collection.mutable

/**
  * @author Alexander Dovzhikov
  */
class WatcherActor extends Actor with ActorLogging {
  val listener = context.actorOf(Props[ListenerActor], "listener")
  val task = new WatcherTask(self, log)
  val thread = new Thread(task, "Watch Thread")
  val directories = mutable.Map[Path, ActorRef]()

  override def preStart() = {
    thread.setDaemon(true)
    thread.start()
  }

  override def postStop() = thread.interrupt()

  def receive = LoggingReceive {
    case MonitorDir(path) =>
      directories += path -> context.actorOf(DirectoryActor.props(path, listener), path.getFileName.toString)
      task.watchDirectory(path)

    case e: WatcherEvent =>
      for (dirActor <- directories get e.path.getParent) dirActor ! e

    case _ =>
  }
}

case class MonitorDir(path: Path)

sealed trait WatcherEvent {
  def path: Path
}

case class EntryCreated(path: Path) extends WatcherEvent
case class EntryModified(path: Path) extends WatcherEvent
case class EntryDeleted(path: Path) extends WatcherEvent

sealed trait Separator
case object CR extends Separator
case object LF extends Separator
case object CRLF extends Separator

case class LineUpdate(path: Path, line: String, separator: Separator)
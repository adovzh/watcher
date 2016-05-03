package dan.watcher

import java.nio.file.{Files, Path}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive

import scala.collection.JavaConverters._

/**
  * @author Alexander Dovzhikov
  */
class DirectoryActor(dir: Path, listener: ActorRef) extends Actor with ActorLogging {

  var fileActors = {
    val stream = Files.newDirectoryStream(dir)

    try {
      val x = for (path <- stream.asScala if !Files.isDirectory(path)) yield actorEntry(path)

      x.toMap
    } finally stream.close()
  }

  private[this] def actorEntry(path: Path) = path -> context.actorOf(FileActor.props(path, listener), path.getFileName.toString)

  def receive = LoggingReceive {
    case m @ EntryCreated(path) =>
      fileActors += actorEntry(path)

    case m @ EntryModified(path) =>
      for (child <- fileActors.get(path)) child.forward(m)

    case _ =>
  }
}

object DirectoryActor {
  def props(dir: Path, listener: ActorRef) = Props(new DirectoryActor(dir, listener))
}
package dan.watcher

import java.nio.file.StandardWatchEventKinds.{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY}
import java.nio.file._

import akka.actor.ActorRef
import akka.event.LoggingAdapter
import grizzled.slf4j.Logging

import scala.collection.JavaConverters._

/**
  * @author Alexander Dovzhikov
  */
class WatcherTask(notifyRef: ActorRef, log: LoggingAdapter) extends Runnable {
  private val watchService = FileSystems.getDefault.newWatchService()

  def watchDirectory(path: Path): Unit =
    path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE)


  def run(): Unit = {
    try {
      while (!Thread.currentThread().isInterrupted) {
        val watchKey = watchService.take()

        for (event <- watchKey.pollEvents.asScala) {
          val path = event.context.asInstanceOf[Path]
          val dir = watchKey.watchable.asInstanceOf[Path]

          log.debug(s"path: $path")
          log.debug(s"dir: $dir")

          val fullPath = dir.resolve(path)
          log.debug(s"fullPath: $fullPath")

          val watcherEvent = event.kind match {
            case ENTRY_CREATE => log.debug("Created"); EntryCreated(fullPath)
            case ENTRY_DELETE => log.debug("Deleted"); EntryDeleted(fullPath)
            case ENTRY_MODIFY => log.debug("Modified"); EntryModified(fullPath)
          }

          notifyRef ! watcherEvent
        }

        watchKey.reset()
      }
    } catch {
      case e: InterruptedException =>
        log.debug("Watcher task is interrupted")
    } finally {
      watchService.close()
    }
  }
}

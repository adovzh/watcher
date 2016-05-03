package dan.watcher

import java.nio.file.{Files, Paths}

import akka.Main.Terminator
import akka.actor.{ActorSystem, PoisonPill, Props}
import grizzled.slf4j.Logging

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.util.control.NonFatal

/**
  * @author Alexander Dovzhikov
  */
object WatcherApp extends Logging {

  def main(args: Array[String]): Unit = {
    println(args.toSeq)
    for (path <- Files.newDirectoryStream(Paths.get(args.head)).asScala) {
      println(path)
    }

    val system = ActorSystem("watcher-app")

    try {
      val watcher = system.actorOf(Props[WatcherActor], "watcher")
      system.actorOf(Props(classOf[Terminator], watcher), "watcher-terminator")

      watcher ! MonitorDir(Paths.get(args.head))

      scala.sys.addShutdownHook {
        import concurrent.duration._
        info("System is going to shut down")

        watcher ! PoisonPill
        Await.result(system.whenTerminated, 5.seconds)
      }
    } catch {
      case NonFatal(e) =>
        system.terminate()
        throw e
    }
  }

}

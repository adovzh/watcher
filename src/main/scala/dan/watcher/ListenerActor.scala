package dan.watcher

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive

/**
  * @author Alexander Dovzhikov
  */
class ListenerActor extends Actor with ActorLogging {
  def receive = LoggingReceive {
    case LineUpdate(path, line, _) =>
      log.info(s"=> $line")
  }
}

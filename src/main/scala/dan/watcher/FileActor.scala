package dan.watcher

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.file.{Files, Path}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive

import scala.annotation.tailrec

/**
  * @author Alexander Dovzhikov
  */
class FileActor(file: Path, listener: ActorRef) extends Actor with ActorLogging {
  import FileActor._

  private[this] val channel = Files.newByteChannel(file)
  private[this] val buffer = ByteBuffer.allocate(BufferSize)
  private[this] val baos = new ByteArrayOutputStream(BufferSize)
  private[this] var lastPosition: Long = readChannel()

  @tailrec
  private def readChannel(): Long = {
    buffer.clear()
    val count = channel.read(buffer)

    if (count == -1) channel.size()
    else {
      // handle buffer
      buffer.flip()

      var state = 1

      while (buffer.hasRemaining) {
        val b = buffer.get()

        state = transition(state, b)
      }

      state match {
        case 2 => sendMessage(CR)
        case 3 => sendMessage(CRLF)
        case 4 => sendMessage(LF)
        case _ =>
      }

      // next iteration
      readChannel()
    }
  }

  private def transition(state: Int, b: Byte): Int = {
    b match {
      case '\r' => state match {
        case 1 => 2
        case 2 => sendMessage(CR); 2
        case 3 => sendMessage(CRLF); 2
        case 4 => sendMessage(LF); 2
      }

      case '\n' => state match {
        case 1 => 4
        case 2 => 3
        case 3 => sendMessage(CRLF); 4
        case 4 => sendMessage(LF); 4
      }

      case _ => state match {
        case 1 => baos.write(b); 1
        case 2 => sendMessage(CR); baos.write(b); 1
        case 3 => sendMessage(CRLF); baos.write(b); 1
        case 4 => sendMessage(LF); baos.write(b); 1
      }
    }
  }

  private def sendMessage(separator: Separator): Unit = {
    listener ! LineUpdate(file, baos.toString("UTF-8"), separator)
    baos.reset()
  }

  def receive = LoggingReceive {
    case EntryModified(_) =>
      log.debug(s"Position: ${channel.position()}")
      log.debug(s"LastPosition: $lastPosition")
      if (lastPosition < channel.position()) channel.position(0)
      lastPosition = readChannel()

    case _ =>
  }

  override def postStop() = channel.close()
}

object FileActor {
  val BufferSize: Int = 4096
  def props(file: Path, listener: ActorRef) = Props(new FileActor(file, listener))
}
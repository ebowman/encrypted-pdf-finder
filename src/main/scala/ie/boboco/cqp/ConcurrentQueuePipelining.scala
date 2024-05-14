package ie.boboco.cqp

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Callable, Executors, LinkedBlockingQueue}
import scala.annotation.targetName

trait ConcurrentQueuePipelining {
  type Processor[T] = (Option[T], LinkedBlockingQueue[Option[T]]) => Unit

  implicit class ProcessableQueue[T](queue: LinkedBlockingQueue[Option[T]]) {

    private val outputQueue = new LinkedBlockingQueue[Option[T]]()

    def >>(processor: Processor[T]): LinkedBlockingQueue[Option[T]] = {
      processStep(1, queue, outputQueue, processor)
      outputQueue
    }

    @targetName("pipeWithThreads")
    def >>(args: (Processor[T], Int)): LinkedBlockingQueue[Option[T]] = {
      processStep(args._2, queue, outputQueue, args._1)
      outputQueue
    }
  }

  implicit class ProcessableItem[T](item: T) {
    private val inputQueue = {
      val queue = new LinkedBlockingQueue[Option[T]]()
      queue.put(Some(item))
      queue.put(None)
      queue
    }
    private val outputQueue = new LinkedBlockingQueue[Option[T]]()

    def >>(processor: Processor[T]): LinkedBlockingQueue[Option[T]] = {
      processStep(1, inputQueue, outputQueue, processor)
      outputQueue
    }

    @targetName("pipeWithThreads")
    def >>(args: (Processor[T], Int)): LinkedBlockingQueue[Option[T]] = {
      processStep(args._2, inputQueue, outputQueue, args._1)
      outputQueue
    }
  }

  // Implement the processStep function
  def processStep[T](threads: Int,
                     inputQueue: LinkedBlockingQueue[Option[T]],
                     outputQueue: LinkedBlockingQueue[Option[T]],
                     worker: (Option[T], LinkedBlockingQueue[Option[T]]) => Unit): Unit = {
    val executor = Executors.newFixedThreadPool(threads)
    val counter = new AtomicInteger(threads)
    for (_ <- 1 to threads) executor.submit(() => {
      var done = false
      while (!done) {
        inputQueue.take().fold {
          done = true
        } { item =>
          worker(Some(item), outputQueue)
        }
      }
      if (counter.decrementAndGet() == 0)
        outputQueue.put(None)
      else
        inputQueue.put(None)
    }.asInstanceOf[Callable[Unit]])
    executor.shutdown()
  }
}

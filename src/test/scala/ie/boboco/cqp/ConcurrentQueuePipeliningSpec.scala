package ie.boboco.cqp

import java.util.concurrent.{Executors, LinkedBlockingQueue}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class ConcurrentQueuePipeliningSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks with ConcurrentQueuePipelining {

  "PipelineQueue" should "process items sequentially with single thread" in {
    val inputQueue = new LinkedBlockingQueue[Option[Int]]()
    val outputQueue = new LinkedBlockingQueue[Option[Int]]()

    (1 to 5).foreach(i => inputQueue.put(Some(i)))
    inputQueue.put(None)

    val processItem: (Option[Int], LinkedBlockingQueue[Option[Int]]) => Unit = {
      case (Some(i), out) => out.put(Some(i * 2))
      case (None, _) => // Do nothing
    }

    val processedQueue = inputQueue >> processItem

    Iterator.continually(processedQueue.take())
      .takeWhile(_.isDefined)
      .flatten
      .toList shouldEqual List(2, 4, 6, 8, 10)
  }

  it should "process items concurrently with multiple threads" in {
    val inputQueue = new LinkedBlockingQueue[Option[Int]]()
    val outputQueue = new LinkedBlockingQueue[Option[Int]]()

    (1 to 10).foreach(i => inputQueue.put(Some(i)))
    inputQueue.put(None)

    val processItem: (Option[Int], LinkedBlockingQueue[Option[Int]]) => Unit = {
      case (Some(i), out) => out.put(Some(i * 2))
      case (None, _) => // Do nothing
    }

    val processedQueue = inputQueue >> (processItem, 4)

    val results = Iterator.continually(processedQueue.take())
      .takeWhile(_.isDefined)
      .flatten
      .toList

    results.sorted shouldEqual (1 to 10).map(_ * 2).toList
  }

  it should "handle empty input queue correctly" in {
    val inputQueue = new LinkedBlockingQueue[Option[Int]]()
    val processItem: (Option[Int], LinkedBlockingQueue[Option[Int]]) => Unit = {
      case (Some(i), out) => out.put(Some(i * 2))
      case (None, _) => // Do nothing
    }

    val processedQueue = inputQueue >> processItem

    val executor = Executors.newFixedThreadPool(1)
    implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(executor)
    Future {
      inputQueue.put(None)
    }

    Iterator.continually(processedQueue.take())
      .takeWhile(_.isDefined)
      .flatten
      .toList shouldEqual List.empty

    executor.shutdown()
  }

  "PipelineItem" should "process a single item with single thread" in {
    val item = 21

    val processItem: (Option[Int], LinkedBlockingQueue[Option[Int]]) => Unit = {
      case (Some(i), out) => out.put(Some(i * 2))
      case (None, _) => // Do nothing
    }

    val processedQueue = item >> processItem

    Iterator.continually(processedQueue.take())
      .takeWhile(_.isDefined)
      .flatten
      .toList shouldEqual List(42)
  }

  it should "process a single item concurrently with multiple threads" in {
    val item = 21

    val processItem: (Option[Int], LinkedBlockingQueue[Option[Int]]) => Unit = {
      case (Some(i), out) => out.put(Some(i * 2))
      case (None, _) => // Do nothing
    }

    val processedQueue = item >> (processItem, 3)

    Iterator.continually(processedQueue.take())
      .takeWhile(_.isDefined)
      .flatten
      .toList shouldEqual List(42)
  }

  it should "process multiple items with a single thread" in {
    val items = {
      val inputQueue = new LinkedBlockingQueue[Option[Int]]()
      for (elem <- 1 to 10) {
        inputQueue.put(Some(elem))
      }
      inputQueue.put(None)
      inputQueue
    }

    val processItem: (Option[Int], LinkedBlockingQueue[Option[Int]]) => Unit = {
      case (Some(i), out) => out.put(Some(i * 2))
      case (None, _) => // Do nothing
    }

    val processedQueue = items >> processItem

    Iterator.continually(processedQueue.take())
      .takeWhile(_.isDefined)
      .flatten
      .toList shouldEqual (1 to 10).map(_ * 2).toList
  }

  it should "process multiple items concurrently with multiple threads" in {
    val items = {
      val inputQueue = new LinkedBlockingQueue[Option[Int]]()
      for (elem <- 1 to 10) {
        inputQueue.put(Some(elem))
      }
      inputQueue.put(None)
      inputQueue
    }

    val processItem: (Option[Int], LinkedBlockingQueue[Option[Int]]) => Unit = {
      case (Some(i), out) => out.put(Some(i * 2))
      case (None, _) => // Do nothing
    }

    val processedQueue = items >> (processItem, 4)

    val results = Iterator.continually(processedQueue.take())
      .takeWhile(_.isDefined)
      .flatten
      .toList

    results.sorted shouldEqual (1 to 10).map(_ * 2).toList
  }

  it should "carry on if the processItem throws an exception" in {
    val inputQueue = new LinkedBlockingQueue[Option[Int]]()
    val outputQueue = new LinkedBlockingQueue[Option[Int]]()

    (1 to 5).foreach(i => inputQueue.put(Some(i)))
    inputQueue.put(None)

    val processItem: (Option[Int], LinkedBlockingQueue[Option[Int]]) => Unit = {
      case (Some(i), out) => if (i == 3) throw new Exception("Boom!") else out.put(Some(i * 2))
      case (None, _) => // Do nothing
    }

    val processedQueue = inputQueue >> processItem

    Iterator.continually(processedQueue.take())
      .takeWhile(_.isDefined)
      .flatten
      .toList shouldEqual List(2, 4, 8, 10)
  }
}

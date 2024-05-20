package ie.boboco.cqp

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Callable, Executors, LinkedBlockingQueue}
import scala.annotation.targetName
import scala.util.{Try, Failure}

/**
 * Provides functionality to pipeline operations on data items concurrently using a queue-based approach.
 * This trait allows for defining pipelines that process items through multiple stages using single or multiple threads.
 * Each stage can be thought of a compute step that can process the data items in parallel, potentially transforming them.
 *
 * The pipelining is implemented by enhancing the functionality of `LinkedBlockingQueue` and individual data items
 * to support operations like `>>` which denote the passage of items through different processing stages.
 * Each stage in the pipeline can be assigned a specific function that processes the items, potentially transforming
 * them, and can operate with a specified number of threads to parallelize the processing. That function is written
 * to be passed an input to the compute task, and the task may produce zero or more outputs, not by returning from
 * the function (which could only happen at the end of the computation), but by placing the output in the output queue
 * as the computation progresses. The output queue is a `LinkedBlockingQueue` that holds the processed items and makes
 * them available to the next stage of the processing pipeline. By tuning the thread sizes per processing step, you can
 * control the level of parallelism in the pipeline to maximize throughput.
 *
 * Usage of this trait and its implicit classes requires an understanding of option types and blocking queues,
 * as well as basic concurrency concepts in Scala and Java, such as threads and atomic operations.
 *
 * Example Usage:
 * {{{
 * val pipeline = new File("/path/to/directory") >>
 *                parallelFindPDFs >>
 *                (enqueuePasswordProtectedPdfs, 8)
 *
 * Iterator.continually(pipeline.take()).takeWhile(_.isDefined).foreach(item => println(item.get))
 * }}}
 *
 * Note: The provided example assumes the existence of properly defined `parallelFindPDFs`
 * and `enqueuePasswordProtectedPdfs` functions, which should be implemented to suit
 * the specific data processing requirements.
 */
trait ConcurrentQueuePipelining {
  private type ItemProcessor[T] = (Option[T], LinkedBlockingQueue[Option[T]]) => Unit

  /**
   * An implicit class that extends the functionality of `LinkedBlockingQueue` with the ability to process items
   * through a defined pipeline. It adds methods for chaining operations, represented by the `>>` operator,
   * which allow the items to be processed sequentially through different stages.
   *
   * The class allows for the creation of a new output queue which holds the result of processing
   * and manages concurrency through the use of multiple threads where specified.
   *
   * @tparam T The type of items being processed in the pipeline.
   * @param queue The input queue holding items to be processed.
   *
   *              Usage:
   * {{{
   * val inputQueue = new LinkedBlockingQueue[Option[Int]]()
   * inputQueue.put(Some(1))
   * inputQueue.put(Some(2))
   * inputQueue.put(None) // A marker to indicate the end of the queue, called a "poison pill".
   *
   * val processedQueue = inputQueue >> processItem >> (additionalProcessing, 3)
   * processedQueue.foreach(println)
   * }}}
   *
   *              Here, `processItem` and `additionalProcessing` would be functions that take an `Option[Int]` and an output
   *              `LinkedBlockingQueue[Option[Int]]` and perform some processing. The item `None` is used to signal the end
   *              of processing in the queue.
   *
   *              The `>>` operator can be used in two forms:
   *  - `queue >> process` to process with a single thread.
   *  - `queue >> (process, n)` to process using `n` threads.
   */
  implicit class PipelineQueue[T](queue: LinkedBlockingQueue[Option[T]]) {
    private val output = new LinkedBlockingQueue[Option[T]]()

    /**
     * Processes items from the input queue using a single thread and passes them to the next stage of the pipeline.
     *
     * @param processor A function that represents the processing logic for each item.
     * @return A new `LinkedBlockingQueue` containing the processed items.
     */
    @targetName("singleThreadPipe")
    def >>(processor: ItemProcessor[T]): LinkedBlockingQueue[Option[T]] = {
      executeConcurrently(1, queue, output, processor)
      output
    }

    /**
     * Processes items from the input queue using a specified number of threads and passes them to the next stage of the pipeline.
     *
     * @param args A tuple containing the processing function and the number of threads to use for processing.
     * @return A new `LinkedBlockingQueue` containing the processed items.
     */
    @targetName("pipeWithThreads")
    def >>(args: (ItemProcessor[T], Int)): LinkedBlockingQueue[Option[T]] = {
      executeConcurrently(args._2, queue, output, args._1)
      output
    }
  }

  /**
   * An implicit class that wraps a single item of type `T` to provide pipeline processing capabilities. This class
   * enables the item to be seamlessly processed through various stages of a pipeline using the `>>` operator.
   * The pipeline operations can be executed with either a single thread or multiple threads, depending on the configuration.
   *
   * @tparam T The type of the item to be processed.
   * @param item The single item that will be wrapped and processed through the pipeline.
   *
   *             Usage:
   * {{{
   * val item = 42
   * val resultQueue = item >> processItem >> (additionalProcessing, 5)
   * resultQueue.foreach(result => println(s"Processed result: $result"))
   * }}}
   *
   *             In this example, `processItem` and `additionalProcessing` are functions designed to handle an `Option[T]` and an
   *             output `LinkedBlockingQueue[Option[T]]`, processing the item and possibly transforming it. The `None` value is
   *             used to signal the end of processing in the pipeline, ensuring proper termination of the pipeline.
   *
   *             The `>>` operator is used here to define how the item should be processed:
   *  - `item >> process` to process using a single thread.
   *  - `item >> (process, n)` to process using `n` threads.
   */
  implicit class PipelineItem[T](item: T) {
    private val input = {
      val queue = new LinkedBlockingQueue[Option[T]]()
      queue.put(Some(item))
      queue.put(None)
      queue
    }
    private val output = new LinkedBlockingQueue[Option[T]]()

    /**
     * Processes the item through a pipeline stage using a single thread and places the result in a new queue.
     *
     * @param processor A function that takes an `Option[T]` and an output queue `LinkedBlockingQueue[Option[T]]`
     *                  to process the item.
     * @return A `LinkedBlockingQueue[Option[T]]` containing the processed item and a None to signal the end.
     */
    @targetName("singleThreadPipe")
    def >>(processor: ItemProcessor[T]): LinkedBlockingQueue[Option[T]] = {
      executeConcurrently(1, input, output, processor)
      output
    }

    /**
     * Processes the item through a pipeline stage using the specified number of threads and places the results in a new queue.
     *
     * @param args A tuple containing the processing function and the number of threads to be used for the processing.
     * @return A `LinkedBlockingQueue[Option[T]]` containing the processed items and a None to signal the end.
     */
    @targetName("pipeWithThreads")
    def >>(args: (ItemProcessor[T], Int)): LinkedBlockingQueue[Option[T]] = {
      executeConcurrently(args._2, input, output, args._1)
      output
    }
  }

  /**
   * Executes a given processing function concurrently across multiple threads. This method manages the concurrent
   * execution of tasks that process items from an input queue and places the processed items into an output queue.
   * It uses a fixed thread pool to achieve parallel processing and ensures that the output queue is properly signaled
   * for completion once all tasks have finished processing.
   *
   * This method is designed to handle potentially blocking operations and exceptions during item processing,
   * ensuring that all items are processed and that errors are logged without halting the overall execution.
   *
   * @tparam T The type of the items in the input and output queues.
   * @param threads The number of threads to use for processing items. This determines the size of the fixed thread pool.
   * @param input   The `LinkedBlockingQueue` from which items will be consumed. Each item is wrapped in an `Option` to handle
   *                the presence of data and to signal the end of the queue (`None`).
   * @param output  The `LinkedBlockingQueue` into which processed items will be placed, also wrapped in `Option` to signal
   *                completion.
   * @param worker  A function that defines how each item should be processed. It takes an `Option[T]` (the item to process)
   *                and a `LinkedBlockingQueue[Option[T]]` (the output queue) as parameters.
   *
   *                Usage:
   * {{{
   * val inputQueue = new LinkedBlockingQueue[Option[Int]](List(Some(1), Some(2), None).asJava)
   * val outputQueue = new LinkedBlockingQueue[Option[Int]]()
   * executeConcurrently(2, inputQueue, outputQueue, (item, out) => {
   *   item.foreach(i => out.put(Some(i * 2)))
   * })
   * // outputQueue will contain processed items and a None to signal the end
   * }}}
   *
   *                Note: It's important to ensure that the `None` to signal the queue's end is only placed once by the last finishing thread.
   */
  private def executeConcurrently[T](threads: Int,
                                     input: LinkedBlockingQueue[Option[T]],
                                     output: LinkedBlockingQueue[Option[T]],
                                     worker: (Option[T], LinkedBlockingQueue[Option[T]]) => Unit): Unit = {
    val executor = Executors.newFixedThreadPool(threads)
    val counter = new AtomicInteger(threads)
    for (_ <- 1 to threads) executor.submit(() => {
      var done = false
      while (!done) {
        input.take().fold {
          done = true
        } { item =>
          Try {
            worker(Some(item), output)
          } match {
            case Failure(e) => println(s"Error processing item $item ${e.getMessage}")
            case _ => () // success
          }
        }
      }
      if (counter.decrementAndGet() == 0)
        output.put(None)
      else
        input.put(None)
    }.asInstanceOf[Callable[Unit]])
    executor.shutdown()
  }
}

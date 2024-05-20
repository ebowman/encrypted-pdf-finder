package ie.boboco.cqp.pdf

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException

import java.io.File
import java.nio.file.Files
import java.util.concurrent.{Executors, LinkedBlockingQueue}
import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

/**
 * Provides a workflow for processing PDF files within a directory structure. This trait defines methods to
 * traverse directories, filter for PDF files, and further filter for password-protected PDF files.
 *
 * Each method within this trait is designed to be used as a stage in a pipelined processing sequence,
 * allowing for the isolation of different filtering and processing tasks. The methods utilize functional
 * programming paradigms to ensure composability and ease of integration with other processing stages.
 *
 * Methods:
 * - `parallelFindPDFs`: Recursively searches for PDF files starting from a given directory and enqueues them.
 * - `enqueuePasswordProtectedPdfs`: Checks if a PDF file is password-protected and enqueues it if true.
 *
 * These methods are intended to be used in a pipeline processing context where each takes an input from a
 * `LinkedBlockingQueue` and outputs to another, facilitating asynchronous and concurrent processing.
 *
 * @see ConcurrentQueuePipelining for examples on how to integrate these methods into a processing pipeline.
 */
trait PdfFileWorkflow {

  /**
   * Checks and enqueues PDF files that are password protected. This method attempts to open each PDF file
   * and uses the response from the PDF library to determine if the file is password protected.
   *
   * @param pdfOpt The PDF file to check.
   * @param output The queue where password-protected PDF files are enqueued if found.
   * @throws InvalidPasswordException if the PDF is password protected.
   * @throws Throwable                for any other errors encountered during processing.
   */
  def enqueuePasswordProtectedPdfs(pdfOpt: Option[File], output: LinkedBlockingQueue[Option[File]]): Unit = {
    for {
      file <- pdfOpt
    } try Loader.loadPDF(file).close() catch {
      case e: InvalidPasswordException => output.put(Some(file))
      case e: Throwable =>
        println(s"Error processing file $file ${e.getMessage}")
        e.printStackTrace()
    }
  }

  /**
   * Recursively searches for PDF files starting from a given directory, processing directories and files in parallel.
   * PDF files found are enqueued into the provided output queue. The function uses a fixed thread pool to manage
   * concurrency and avoid blocking the main thread during file processing.
   *
   * @param pdfOpt The initial directory to start the search from, wrapped in an Option.
   * @param output The queue where found PDF files are enqueued.
   */
  def parallelFindPDFs(pdfOpt: Option[File], output: LinkedBlockingQueue[Option[File]]): Unit = {
    val ignoreSuffixes: Set[String] = Set(".epub", ".fcpbundle", ".auh", ".app")

    def processItem(item: File): Seq[File] = {
      item match {
        // Return the directory listing if this is an appropriate directory
        case dir if dir.isDirectory &&
          !Files.isSymbolicLink(dir.toPath) &&
          !ignoreSuffixes.exists(suffix => dir.getName.toLowerCase.endsWith(suffix)) =>
          Option(dir.listFiles()).fold {
            System.err.println("Error: null file list for " + dir.getAbsolutePath)
            Seq.empty
          }{ _.toSeq }

        // Send the file downstream if it's a PDF
        case file if file.isFile &&
          !Files.isSymbolicLink(file.toPath) &&
          file.getName.toLowerCase.endsWith(".pdf") =>
          output.put(Some(file))
          Seq.empty

        // Anything else is ignored
        case _ => Seq.empty
      }
    }

    val executor = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
    implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(executor)

    @tailrec
    def recurse(curDirs: Seq[File]): Unit = {
      if (curDirs.nonEmpty) {
        recurse(
          Await.result(
            Future.sequence(
              curDirs.map(curDir => Future(processItem(curDir)))
            ), Duration.Inf
          ).flatten
        )
      }
    }

    recurse(pdfOpt.toSeq)
    executor.shutdown()
  }
}

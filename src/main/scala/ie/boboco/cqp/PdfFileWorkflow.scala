package ie.boboco.cqp

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException

import java.io.{File, FileFilter, IOException}
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}
import java.util.concurrent.LinkedBlockingQueue

/**
 * Provides a workflow for processing PDF files within a directory structure. This trait defines methods to
 * traverse directories, filter for PDF files, and further filter for password-protected PDF files.
 *
 * Each method within this trait is designed to be used as a stage in a pipelined processing sequence,
 * allowing for the isolation of different filtering and processing tasks. The methods utilize functional
 * programming paradigms to ensure composability and ease of integration with other processing stages.
 *
 * Methods:
 * - `enqueueRelevantDirectories`: Traverses the file system starting from a given root directory and enqueues
 * all relevant directories that do not match specified exclusion criteria.
 * - `enqueuePdfFiles`: Filters for PDF files within a given directory and enqueues them for further processing.
 * - `enqueuePasswordProtectedPdfs`: Checks if a PDF file is password-protected and enqueues it if true.
 *
 * These methods are intended to be used in a pipeline processing context where each takes an input from a
 * `LinkedBlockingQueue` and outputs to another, facilitating asynchronous and concurrent processing.
 *
 * @see ConcurrentQueuePipelining for examples on how to integrate these methods into a processing pipeline.
 */
trait PdfFileWorkflow {

  /**
   * Enqueues directories for further processing, excluding those with specified suffixes that are deemed irrelevant.
   * This method walks the file tree starting from a provided root directory and applies filtering based on the suffix
   * of each directory's name.
   *
   * @param rootFile The root directory from which directory traversal begins.
   * @param output   The queue where directories that pass the filter are enqueued.
   * @throws IOException if an I/O error is encountered during file traversal.
   */
  def enqueueRelevantDirectories(rootFile: Option[File], output: LinkedBlockingQueue[Option[File]]): Unit = {
    val ignoreSuffixes = Set(".epub", ".fcpbundle", ".auh")
    for (file <- rootFile) {
      val visitor = new SimpleFileVisitor[Path] {
        override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
          if (ignoreSuffixes.exists(dir.toFile.getName.toLowerCase.endsWith)) FileVisitResult.SKIP_SUBTREE
          else {
            output.put(Some(dir.toFile))
            FileVisitResult.CONTINUE
          }
        }

        override def visitFileFailed(file: Path, exc: IOException): FileVisitResult = FileVisitResult.CONTINUE
      }

      Files.walkFileTree(file.toPath, visitor)
    }
  }

  /**
   * Filters and enqueues PDF files found in a specified directory. This method checks each file in the directory
   * to determine if it ends with the `.pdf` extension, enqueuing those that do for further processing.
   *
   * @param dirOpt The directory to search for PDF files.
   * @param output The queue where PDF files are enqueued.
   */
  def enqueuePdfFiles(dirOpt: Option[File], output: LinkedBlockingQueue[Option[File]]): Unit = {
    for {
      dir <- dirOpt
      files <- Option(dir.listFiles(new FileFilter {
        override def accept(file: File): Boolean = file.isFile && file.getName.toLowerCase.endsWith(".pdf")
      })) 
      file <- files 
    } output.put(Some(file))
  }

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
    val logErrors = false
    for {
      file <- pdfOpt
    } try PDDocument.load(file).close() catch {
      case e: InvalidPasswordException => output.put(Some(file))
      case e: Throwable => if (logErrors) println(s"Error processing file $file ${e.getMessage}")
    }
  }
}

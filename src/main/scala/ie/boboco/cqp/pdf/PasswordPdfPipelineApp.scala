package ie.boboco.cqp.pdf

import ie.boboco.cqp.ConcurrentQueuePipelining

import java.io.File
import java.util.Date
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.{Level, Logger}

/**
 * The PasswordPdfPipelineApp object provides the main application entry point for processing PDF files
 * within a directory structure. It sets up a concurrent pipeline to traverse directories, identify PDF files,
 * and filter out password-protected PDFs.
 *
 * This application leverages the ConcurrentQueuePipelining and PdfFileWorkflow traits to define and execute
 * the pipeline stages in a concurrent manner, maximizing the utilization of available CPU cores.
 */
object PasswordPdfPipelineApp extends ConcurrentQueuePipelining with PdfFileWorkflow {
  Logger.getLogger("org.apache.pdfbox").setLevel(Level.OFF)

  def main(args: Array[String]): Unit = {
    val rootDir = new File(args.head)
    val coreCount = Runtime.getRuntime.availableProcessors()
    val pdfPipeline = rootDir >> parallelFindPDFs >> (enqueuePasswordProtectedPdfs, coreCount)

    println(new Date())
    val start = System.currentTimeMillis()

    // Process the items in the queue and print the password-protected PDF files
    Iterator.continually(pdfPipeline.take())
      .takeWhile(_.isDefined)
      .flatten
      .foreach(println)

    val end = System.currentTimeMillis()
    println(s"Elapsed: ${end - start} ms")
    println(new Date())
  }
}

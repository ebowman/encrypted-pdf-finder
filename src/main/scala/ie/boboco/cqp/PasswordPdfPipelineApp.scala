package ie.boboco.cqp

import java.io.File
import java.util.Date
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.{Level, Logger}

object PasswordPdfPipelineApp extends App with ConcurrentQueuePipelining with PdfFileWorkflow {
  Logger.getLogger("org.apache.pdfbox").setLevel(Level.OFF) // vs SEVERE

  {
    val rootDir = new File("/Users/ebowman")
    val encPdfQueue = rootDir >> enqueueRelevantDirectories >> enqueuePdfFiles >> (enqueuePasswordProtectedPdfs, 8)

    println(new Date())
    val start = System.currentTimeMillis()
    Iterator.continually(encPdfQueue.take())
      .takeWhile(_.isDefined)
      .foreach(pdf => println("***" + pdf.get))
    val end = System.currentTimeMillis()
    println(s"Time taken: ${end - start} ms")
    println(new Date())
  }
}
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException

import java.io.{File, IOException}
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Callable, Executors, LinkedBlockingQueue}
import java.util.logging.{Level, Logger}


object PDFFinder {

  def main(args: Array[String]): Unit = {
    Logger.getLogger("org.apache.pdfbox").setLevel(Level.SEVERE)

    val rootQueue = {
      val rootFile = new File("/Users/ebowman/Downloads")
      val queue = new LinkedBlockingQueue[Option[File]]()
      queue.put(Some(rootFile))
      queue.put(None)
      queue
    }
    val dirQueue = new LinkedBlockingQueue[Option[File]]
    val pdfQueue = new LinkedBlockingQueue[Option[File]]()
    val encPdfQueue = new LinkedBlockingQueue[Option[File]]()

    processStep[File](1, rootQueue, dirQueue, publishDirectories)
    processStep[File](1, dirQueue, pdfQueue, publishPDFs)
    processStep[File](8, pdfQueue, encPdfQueue, publishEncrypted)

    // process PDF files (on the main thread as they become available)
    println(new Date())
    Iterator.continually(encPdfQueue.take())
      .takeWhile(_.isDefined)
      .foreach(pdf => println("***" + pdf.get))
    println(new Date())
  }

  def publishDirectories(rootFile: Option[File], outputQueue: LinkedBlockingQueue[Option[File]]): Unit = {
    val ignoreSuffixes = Set(".epub", ".fcpbundle", ".auh")
    for {file <- rootFile} {
      Files.walkFileTree(file.toPath, new SimpleFileVisitor[Path] {
        override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
          if (ignoreSuffixes.exists(dir.toFile.getName.toLowerCase.endsWith)) FileVisitResult.SKIP_SUBTREE
          else {
            outputQueue.put(Some(dir.toFile))
            FileVisitResult.CONTINUE
          }
        }

        override def visitFileFailed(file: Path, exc: IOException): FileVisitResult = FileVisitResult.CONTINUE
      })
    }

    println("Done traversing directories")
    outputQueue.put(None)
  }

  def processStep[T](threads: Int,
                     inputQueue: LinkedBlockingQueue[Option[T]],
                     outputQueue: LinkedBlockingQueue[Option[T]],
                     worker: (item: Option[T], output: LinkedBlockingQueue[Option[T]]) => Unit): Unit = {
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

  def publishPDFs(dirOpt: Option[File], outputQueue: LinkedBlockingQueue[Option[File]]): Unit = {
    for {
      dir <- dirOpt
      files <- Option(dir.listFiles())
      file <- files if file.getName.toLowerCase.endsWith(".pdf")
    } outputQueue.put(Some(file))
  }

  def publishEncrypted(pdfOpt: Option[File], outputQueue: LinkedBlockingQueue[Option[File]]): Unit = {
    pdfOpt.foreach { file =>
      try PDDocument.load(file).close() catch {
        case e: InvalidPasswordException => outputQueue.put(Some(file))
        case e: Throwable => println(s"Error processing file $file ${e.getMessage}")
      }
    }
  }
}

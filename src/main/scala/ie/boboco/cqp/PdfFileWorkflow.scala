package ie.boboco.cqp

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException

import java.io.{File, IOException}
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}
import java.util.concurrent.LinkedBlockingQueue

trait PdfFileWorkflow {

  def enqueueRelevantDirectories(rootFile: Option[File], outputQueue: LinkedBlockingQueue[Option[File]]): Unit = {
    val ignoreSuffixes = Set(".epub", ".fcpbundle", ".auh")
    for {file <- rootFile} {
      val visitor = new SimpleFileVisitor[Path] {
        override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
          if (ignoreSuffixes.exists(dir.toFile.getName.toLowerCase.endsWith)) FileVisitResult.SKIP_SUBTREE
          else {
            outputQueue.put(Some(dir.toFile))
            FileVisitResult.CONTINUE
          }
        }

        override def visitFileFailed(file: Path, exc: IOException): FileVisitResult = FileVisitResult.CONTINUE
      }

      Files.walkFileTree(file.toPath, visitor)
    }
  }

  def enqueuePdfFiles(dirOpt: Option[File], outputQueue: LinkedBlockingQueue[Option[File]]): Unit = {
    for {
      dir <- dirOpt
      files <- Option(dir.listFiles())
      file <- files if file.getName.toLowerCase.endsWith(".pdf")
    } outputQueue.put(Some(file))
  }

  def enqueuePasswordProtectedPdfs(pdfOpt: Option[File], outputQueue: LinkedBlockingQueue[Option[File]]): Unit = {
    pdfOpt.foreach { file =>
      try PDDocument.load(file).close() catch {
        case e: InvalidPasswordException => outputQueue.put(Some(file))
        case e: Throwable => println(s"Error processing file $file ${e.getMessage}")
      }
    }
  }
}

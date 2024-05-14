import org.apache.pdfbox.pdmodel.PDDocument

import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import scala.concurrent.*
import scala.util.{Failure, Success}
import ExecutionContext.Implicits.global
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException

object PdfProcessor {
  def main(args: Array[String]): Unit = {
    val rootPath = Paths.get("/Users/ebowman/Downloads")

    val pdfFiles = findPDFFiles(rootPath)

    val futureMatches = processFiles(pdfFiles)

    // Wait for all processing to complete and print results
    futureMatches.onComplete {
      case Success(matches) => matches.foreach(println)
      case Failure(e) => e.printStackTrace()
    }
    Await.result(futureMatches, duration.Duration.Inf)
  }

  def findPDFFiles(rootPath: Path): List[File] = {
    val matcher = FileSystems.getDefault.getPathMatcher("glob:**/*.pdf")
    var pdfFiles: List[File] = List()

    // Custom file visitor to find PDF files
    val visitor = new SimpleFileVisitor[Path] {
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        if (matcher.matches(file)) pdfFiles = file.toFile :: pdfFiles
        FileVisitResult.CONTINUE
      }
    }

    // Traverse the file system
    Files.walkFileTree(rootPath, visitor)
    pdfFiles
  }

  def processFiles(pdfFiles: Seq[File]): Future[Seq[File]] = {
    val futures = pdfFiles.map { file =>
      Future {
        try {
          val document = PDDocument.load(file)
          try None
          finally document.close()
        }
        catch {
          case e: InvalidPasswordException =>
            Some(file)
        }
      }
    }

    // Combine futures to a single future containing a list of successful matches
    Future.sequence(futures).map {
      _.collect { case Some(file) => file }
    }
  }
}

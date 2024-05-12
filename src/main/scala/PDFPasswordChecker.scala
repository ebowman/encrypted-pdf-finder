import org.apache.pdfbox.pdmodel.PDDocument
import zio.*

import java.io.File
import scala.jdk.CollectionConverters.*
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException

object PDFPasswordChecker extends zio.App {

  def findPDFs(directory: File): Task[List[File]] = {
    // Obtain all files and directories within the current directory
    val filesAndDirs = Task(directory.listFiles()).map(_.toList.filter(_ != null))

    // Separate files and directories
    val pdfFiles = filesAndDirs.flatMap(all => Task(all.filter(f => f.isFile && f.getName.endsWith(".pdf"))))
    val dirs = filesAndDirs.flatMap(all => Task(all.filter(_.isDirectory)))

    // Recursively find PDFs in subdirectories
    for {
      files <- pdfFiles
      directories <- dirs
      nestedFiles <- ZIO.foreach(directories)(findPDFs).map(_.flatten)
    } yield files ++ nestedFiles
  }

  def isPasswordProtected(file: File): Task[Boolean] = Task {
    val document = PDDocument.load(file)
    try false
    finally document.close()
  }.catchSome {
    case _: InvalidPasswordException => Task.succeed(true)
  }

  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val rootPath = new File("/Users/ebowman/Downloads")
    findPDFs(rootPath).flatMap { files =>
      ZIO.foreachPar(files) { file =>
        isPasswordProtected(file).flatMap { isProtected =>
          if (isProtected) console.putStrLn(s"${file.getPath} is password protected")
          else ZIO.unit
        }
      }
    }.exitCode
  }
}

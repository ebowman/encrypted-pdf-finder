import zio.*
import zio.console.*

import java.io.File
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException

object PDFPasswordChecker extends zio.App {

  def processFile(file: File): ZIO[Console, Throwable, Unit] = {
    isPasswordProtected(file).flatMap { isProtected =>
      if (isProtected) putStrLn(s"${file.getPath} is password protected")
      else ZIO.unit
    }
  }

  def findAndProcessPDFs(directory: File): ZIO[Console, Throwable, Unit] = {
    if (directory.isDirectory) println(s"Processing directory: ${directory.getPath}")
    val filesAndDirs = Task(Option(directory.listFiles()).getOrElse(Array.empty[File]))

    for {
      all <- filesAndDirs
      pdfFiles = all.filter(f => f.isFile && f.getName.endsWith(".pdf"))
      dirs = all.filter(_.isDirectory)
      _ <- ZIO.foreachPar(pdfFiles)(processFile)
      _ <- ZIO.foreachPar(dirs)(findAndProcessPDFs)
    } yield ()
  }

  def isPasswordProtected(file: File): Task[Boolean] = Task {
    val document = PDDocument.load(file)
    try false
    finally document.close()
  }.catchSome {
    case _: InvalidPasswordException => Task.succeed(true)
  }

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    val rootPath = new File("/Users/ebowman/")
    findAndProcessPDFs(rootPath).exitCode
}

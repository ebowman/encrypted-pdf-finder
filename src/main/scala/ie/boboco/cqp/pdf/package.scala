package ie.boboco.cqp

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException

import java.io.File
import scala.util.{Failure, Success, Try}

package object pdf {
  def isPasswordProtected(file: File): Try[Boolean] = {
    try {
      Loader.loadPDF(file).close()
      Success(false)
    } catch {
      case _: InvalidPasswordException => Success(true)
      case e: Throwable => Failure(e)
    }
  }

}
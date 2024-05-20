package ie.boboco.cqp.pdf

import java.io.{ByteArrayOutputStream, File, PrintStream}
import java.nio.file.{Files, Paths}
import java.util.concurrent.LinkedBlockingQueue

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfter

class PasswordPdfPipelineAppSpec extends AnyFlatSpec with Matchers with BeforeAndAfter {

  private val testDir = Paths.get("src/test/resources/test_pdfs").toAbsolutePath

  "PasswordPdfPipelineApp" should "identify and print password-protected PDFs" in {
    val protectedPdf = new File(testDir.toString, "protected/protected.pdf")
    val nonProtectedPdf = new File(testDir.toString, "non_protected/non-protected.pdf")
    val badPdf = new File(testDir.toString, "bad_pdfs/bad.pdf")

    // Verify that the files exist before running the test
    assert(protectedPdf.exists(), s"${protectedPdf.getAbsolutePath} does not exist")
    assert(nonProtectedPdf.exists(), s"${nonProtectedPdf.getAbsolutePath} does not exist")
    assert(badPdf.exists(), s"${badPdf.getAbsolutePath} does not exist")

    val outContent = new ByteArrayOutputStream()
    val printStream = new PrintStream(outContent)
    try {
      Console.withOut(printStream) {
        PasswordPdfPipelineApp.main(Array(testDir.toString))
      }
    } finally {
      printStream.close()
    }

    val output = outContent.toString
    output should include(protectedPdf.getAbsolutePath)
    output should not include nonProtectedPdf.getAbsolutePath
    output should include(s"${badPdf.getName} Error")
    output should include("Elapsed")
  }
}

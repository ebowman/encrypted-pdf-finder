package ie.boboco.cqp.pdf

import org.scalacheck.Gen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import java.io.{File, FileOutputStream, RandomAccessFile}
import java.nio.file.Files

import scala.util.{Success, Failure}

// If you want to use ScalaTest to run this property test
class PdfUtilsPropertyTest extends AnyFlatSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  "PdfUtils" should "find the trailer at the correct offset" in {
    val trailer: Array[Byte] = "trailer".getBytes

    val fileAndOffsetGen: Gen[(Int, Int)] = for {
      fileSize <- Gen.choose(32, 512)
      offset <- Gen.choose(0, fileSize - trailer.length)
    } yield (fileSize, offset)

    forAll(fileAndOffsetGen) { case (fileSize, offset) =>
      // Create a temporary file
      val tempFile: File = Files.createTempFile("testFile", ".bin").toFile
      tempFile.deleteOnExit()

      // Fill the file with random bytes
      val randomBytes = new Array[Byte](fileSize)
      scala.util.Random.nextBytes(randomBytes)

      // Insert the trailer at the offset
      System.arraycopy(trailer, 0, randomBytes, offset, trailer.length)

      val fos = new FileOutputStream(tempFile)
      fos.write(randomBytes)
      fos.close()

      // Test the PdfUtils.searchBackwardsForSequence method
      val rasFile = new RandomAccessFile(tempFile, "r")
      val result = try {
        PdfUtils.searchBackwardsForSequence(rasFile, trailer)
      } finally {
        tempFile.delete()
        rasFile.close()
      }

      // Verify that the result matches the expected offset
      result should contain(offset.toLong)
    }
  }

  it should "behave correctly when trailer is not present" in {
    val trailer: Array[Byte] = "trailer".getBytes

    // Generator for file sizes
    val fileSizeGen: Gen[Int] = Gen.choose(32, 512)

    forAll(fileSizeGen) { fileSize =>
      // Create a temporary file
      val tempFile: File = Files.createTempFile("testFile", ".bin").toFile
      tempFile.deleteOnExit()

      // Fill the file with random bytes
      val randomBytes = new Array[Byte](fileSize)
      scala.util.Random.nextBytes(randomBytes)

      // Ensure the trailer is not present in the randomBytes
      while (new String(randomBytes).contains("trailer")) {
        scala.util.Random.nextBytes(randomBytes)
      }

      val fos = new FileOutputStream(tempFile)
      fos.write(randomBytes)
      fos.close()

      val rasFile = new RandomAccessFile(tempFile, "r")
      val result = try {
        PdfUtils.searchBackwardsForSequence(rasFile, trailer)
      } finally {
        rasFile.close()
        tempFile.delete()
      }
      result.isEmpty `shouldBe` true
    }
  }
  it should "load a dictionary" in {
    val pdfFile = new File("src/test/resources/test_pdfs/protected/protected.pdf")
    val result = PdfUtils.isPasswordProtected(pdfFile)
    println(result)
  }

  it should "read a dictionary correctly" in {
    val pdfFile = new File("src/test/resources/test_pdfs/protected/protected.pdf")
    val file = new RandomAccessFile(pdfFile, "r")
    file.seek(0L)

    val dict = PdfUtils.readPdfObject(file, 8044)
    PdfDictionaryParser.parsePdfDictionary(dict) match {
      case Success(dict) =>
        println(dict.entries)
        dict.entries.get("Filter") match {
          case Some(PdfName("Standard")) => println("Standard encryption")
          dict.entries.get("R") match {
            case Some(PdfNumber(n)) if n.toInt == 2 => println("128-bit encryption")
            case Some(PdfNumber(n)) if n.toInt == 3 => println("256-bit encryption")
            case Some(PdfNumber(n)) if n.toInt == 4 => println("Password-based encryption")
            case unknown => println(s"Unknown encryption type: $unknown")
          }
          case _ =>
        }
      case Failure(e) => e.printStackTrace()
    }
  }

  it should "read a very specific file" in {
    // val file = new File("/Users/ebowman/Downloads/Bowman fehlende Unterlagen.pdf")
    // val file = new File("/Users/ebowman/Downloads/CompactCalendar2023-PDF-A4/CompactCalendar-ms-a4-2024.pdf")
    // val file = new File("/Users/ebowman/Downloads/Eric Bowman NDA.docx - signed.pdf")
    val file = new File("/Users/ebowman/Downloads/CV 2023-1/Contentful EVP of Engineering - Job Description.pdf")
    val result = PdfUtils.isPasswordProtected(file)
    println(result)
  }

}
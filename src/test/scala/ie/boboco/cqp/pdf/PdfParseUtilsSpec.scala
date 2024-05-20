package ie.boboco.cqp.pdf

import java.io.{File, RandomAccessFile}
import java.nio.file.Files

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PdfParseUtilsSpec extends AnyFlatSpec with Matchers {

  "FileUtils.readFromNextNewline" should "correctly extract the line between newlines" in {
    val content =
      """This is a test file.
        |It contains multiple lines.
        |Each line is separated by a newline character.
        |This is the line we want to extract.
        |Another line after.
        |""".stripMargin

    val tempFile = File.createTempFile("testFile", ".txt")
    Files.write(tempFile.toPath, content.getBytes)
    val file = new RandomAccessFile(tempFile, "r")
    try {
      val position = content.indexOf("Each line is separated by a newline character.") + 1
      val result = PdfParseUtils.readFromNextNewline(file, position)
      result shouldEqual "This is the line we want to extract.\nAnother line after.\n"
    } finally {
      file.close()
      tempFile.delete()
    }
  }

  it should "return an empty string if there is no following newline" in {
    val content = "This is a single line without a newline at the end"
    val tempFile = File.createTempFile("testFile", ".txt")
    Files.write(tempFile.toPath, content.getBytes)
    val file = new RandomAccessFile(tempFile, "r")
    try {
      val position = 0
      val result = PdfParseUtils.readFromNextNewline(file, position)
      result shouldEqual ""
    } finally {
      file.close()
      tempFile.delete()
    }
  }

  it should "handle multiple consecutive newlines correctly" in {
    val content = "First line.\n\n\nFourth line after three newlines.\n"
    val tempFile = File.createTempFile("testFile", ".txt")
    Files.write(tempFile.toPath, content.getBytes)
    val file = new RandomAccessFile(tempFile, "r")
    try {
      val position = content.indexOf("\n\n") + 2
      val result = PdfParseUtils.readFromNextNewline(file, position)
      result shouldEqual "Fourth line after three newlines.\n"
    } finally {
      file.close()
      tempFile.delete()
    }
  }

  it should "handle empty lines correctly" in {
    val content = "First line.\n\nSecond line after an empty line.\n"
    val tempFile = File.createTempFile("testFile", ".txt")
    Files.write(tempFile.toPath, content.getBytes)
    val file = new RandomAccessFile(tempFile, "r")
    try {
      val position = content.indexOf("\n")
      val result = PdfParseUtils.readFromNextNewline(file, position)
      result shouldEqual "\nSecond line after an empty line.\n"

      val position2 = content.indexOf("\n") + 1
      val result2 = PdfParseUtils.readFromNextNewline(file, position2)
      result2 shouldEqual "Second line after an empty line.\n"
    } finally {
      file.close()
      tempFile.delete()
    }
  }

  it should "handle end of file correctly" in {
    val content = "First line.\nSecond line after an empty line."
    val tempFile = File.createTempFile("testFile", ".txt")
    Files.write(tempFile.toPath, content.getBytes)
    val file = new RandomAccessFile(tempFile, "r")
    try {
      val position = content.indexOf("\n")
      val result = PdfParseUtils.readFromNextNewline(file, position)
      result shouldEqual "Second line after an empty line."
    } finally {
      file.close()
      tempFile.delete()
    }
  }
}

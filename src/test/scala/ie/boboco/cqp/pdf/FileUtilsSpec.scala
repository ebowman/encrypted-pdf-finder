package ie.boboco.cqp.pdf

import java.io.{File, RandomAccessFile}
import java.nio.file.Files
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FileUtilsSpec extends AnyFlatSpec with Matchers {

  "FileUtils.searchForSequence" should "find the sequence at the correct position" in {
    val content = "This is a test file with a target sequence in it."
    val sequence = "target sequence".getBytes("UTF-8")

    val tempFile = File.createTempFile("testFile", ".txt")
    Files.write(tempFile.toPath, content.getBytes("UTF-8"))

    val file = new RandomAccessFile(tempFile, "r")
    try {
      val position = FileUtils.searchForSequence(file, sequence)
      position shouldEqual content.indexOf("target sequence")
    } finally {
      file.close()
      tempFile.delete()
    }
  }

  it should "return -1 if the sequence is not found" in {
    val content = "This is a test file without the sequence."
    val sequence = "nonexistent sequence".getBytes("UTF-8")

    val tempFile = File.createTempFile("testFile", ".txt")
    Files.write(tempFile.toPath, content.getBytes("UTF-8"))

    val file = new RandomAccessFile(tempFile, "r")
    try {
      val position = FileUtils.searchForSequence(file, sequence)
      position shouldEqual -1
    } finally {
      file.close()
      tempFile.delete()
    }
  }

  it should "find the sequence at the beginning of the file" in {
    val content = "target sequence at the beginning of the file."
    val sequence = "target sequence".getBytes("UTF-8")

    val tempFile = File.createTempFile("testFile", ".txt")
    Files.write(tempFile.toPath, content.getBytes("UTF-8"))

    val file = new RandomAccessFile(tempFile, "r")
    try {
      val position = FileUtils.searchForSequence(file, sequence)
      position shouldEqual 0
    } finally {
      file.close()
      tempFile.delete()
    }
  }

  it should "find the sequence at the end of the file" in {
    val content = "The sequence is at the end of the file target sequence."
    val sequence = "target sequence".getBytes("UTF-8")

    val tempFile = File.createTempFile("testFile", ".txt")
    Files.write(tempFile.toPath, content.getBytes("UTF-8"))

    val file = new RandomAccessFile(tempFile, "r")
    try {
      val position = FileUtils.searchForSequence(file, sequence)
      position shouldEqual content.indexOf("target sequence")
    } finally {
      file.close()
      tempFile.delete()
    }
  }
}

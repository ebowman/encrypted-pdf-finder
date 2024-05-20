package ie.boboco.cqp.pdf

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.File
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.{Files, Path}
import java.util.concurrent.LinkedBlockingQueue

class PdfFileWorkflowSpec extends AnyFlatSpec with Matchers with ScalaFutures with PdfFileWorkflow {

  "enqueuePasswordProtectedPdfs" should "enqueue password protected PDFs" in {
    val outputQueue = new LinkedBlockingQueue[Option[File]]()
    val protectedPdf = new File("src/test/resources/test_pdfs/protected/protected.pdf")

    enqueuePasswordProtectedPdfs(Some(protectedPdf), outputQueue)

    outputQueue.size() shouldEqual 1
    outputQueue.take() shouldEqual Some(protectedPdf)
  }

  it should "not enqueue non-password protected PDFs" in {
    val outputQueue = new LinkedBlockingQueue[Option[File]]()
    val nonProtectedPdf = new File("src/test/resources/test_pdfs/non_protected/non_protected.pdf")

    enqueuePasswordProtectedPdfs(Some(nonProtectedPdf), outputQueue)

    outputQueue.size() shouldEqual 0
  }

  "parallelFindPDFs" should "find and enqueue PDF files" in {
    val rootDir = new File("src/test/resources/test_pdfs")
    rootDir.exists() shouldBe true
    val outputQueue = new LinkedBlockingQueue[Option[File]]()

    parallelFindPDFs(Some(rootDir), outputQueue)
    outputQueue.put(None)
    val files = Iterator.continually(outputQueue.take()).takeWhile(_.isDefined).flatten.toSeq
    files.map(_.getName).sorted shouldEqual Seq("bad.pdf", "non-protected.pdf", "protected.pdf", "test1.pdf", "test2.pdf")
  }

  it should "ignore non-PDF files and symbolic links" in {
    val rootDir = new File("src/test/resources/test_pdfs")
    val outputQueue = new LinkedBlockingQueue[Option[File]]()

    // Create a symbolic link
    val symLink = new File(rootDir, "dir1/symlink")
    Files.createSymbolicLink(symLink.toPath, Path.of("../dir2"))

    try {
      val symLink2 = new File(rootDir, "dir1/symlink.pdf")
      Files.createSymbolicLink(symLink2.toPath, Path.of("../dir2/test2.pdf"))
      try {
        parallelFindPDFs(Some(rootDir), outputQueue)
        outputQueue.put(None)
        val files = Iterator.continually(outputQueue.take()).takeWhile(_.isDefined).flatten.toSeq
        files.map(_.getName).sorted shouldEqual Seq("bad.pdf", "non-protected.pdf", "protected.pdf", "test1.pdf", "test2.pdf")
      } finally {
        Files.delete(symLink2.toPath)
      }
    } finally {
      Files.delete(symLink.toPath)
    }
  }
  it should "process an empty directory correctly" in {
    val rootDir = new File("src/test/resources/test_pdfs/empty")
    rootDir.mkdirs()
    rootDir.exists() shouldBe true
    val outputQueue = new LinkedBlockingQueue[Option[File]]()

    parallelFindPDFs(Some(rootDir), outputQueue)
    outputQueue.put(None)
    val files = Iterator.continually(outputQueue.take()).takeWhile(_.isDefined).flatten.toSeq
    files shouldBe empty
  }

  it should "process a directory it doesn't have permission to read correctly" in {
    val rootDir = new File("src/test/resources/test_pdfs/empty")
    val outputQueue = new LinkedBlockingQueue[Option[File]]()
    val prevPermissions = Files.getPosixFilePermissions(rootDir.toPath)
    val permissions = PosixFilePermissions.fromString("---------")
    Files.setPosixFilePermissions(rootDir.toPath, permissions)
    try {
      parallelFindPDFs(Some(rootDir), outputQueue)
      outputQueue.put(None)
      val files = Iterator.continually(outputQueue.take()).takeWhile(_.isDefined).flatten.toSeq
      files shouldBe empty
    } finally {
      Files.setPosixFilePermissions(rootDir.toPath, prevPermissions)
    }
  }
}

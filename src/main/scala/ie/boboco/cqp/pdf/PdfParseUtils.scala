package ie.boboco.cqp.pdf

import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import scala.annotation.tailrec

object PdfParseUtils {

  def readFromNextNewline(file: RandomAccessFile, position: Long): String = {
    // Helper function to find the next newline position
    @annotation.tailrec
    def findNextNewline(pos: Long): Long = {
      if (pos >= file.length()) -1L
      else {
        file.seek(pos)
        val byte = file.readByte()
        if (byte == 0x0a || byte == 0x0d) pos
        else findNextNewline(pos + 1)
      }
    }

    // Find the next newline character position
    val nextNewlinePos = findNextNewline(position)
    if (nextNewlinePos == -1L) return ""

    // Move to the position one byte past the next newline
    val startPos = nextNewlinePos + 1
    file.seek(startPos)

    // Read the remaining bytes from the file
    val remainingBytes = new Array[Byte]((file.length() - startPos).toInt)
    file.readFully(remainingBytes)

    // Convert the bytes to a string using ISO-8859-1 encoding
    new String(remainingBytes, StandardCharsets.ISO_8859_1)
  }
}
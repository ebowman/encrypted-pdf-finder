package ie.boboco.cqp.pdf

import java.io.{File, RandomAccessFile}
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object PdfUtils {

  def isPasswordProtected(pdfFile: File): Try[Boolean] = {
    println(s"Checking $pdfFile")
    // open a random access file for pdfFile
    val file = new RandomAccessFile(pdfFile, "r")
    try {
      val posOpt = searchBackwardsForSequence(file, "trailer".getBytes("ISO-8859-1"), 4096)
      posOpt.map { pos =>
        val dictString = PdfParseUtils.readFromNextNewline(file, pos)
        PdfDictionaryParser.parsePdfDictionary(dictString) match {
          case Success(dict) if dict.entries.contains("Encrypt") =>
            val enc = dict.entries("Encrypt").asInstanceOf[PdfReference]
            val query = s"${enc.objNum} ${enc.genNum} obj"
            FileUtils.searchForSequence(file, query.getBytes("ISO-8859-1")) match {
              case -1L => Success(false)
              case pos =>
                println(s"Found Encrypt object at ${java.lang.Long.toHexString(pos)}")
                val dict = PdfUtils.readPdfObject(file, pos)
                PdfDictionaryParser.parsePdfDictionary(dict) match {
                  case Success(dict) =>
                    println(dict.entries)
                    println(dict.entries.get("U"))
                    dict.entries.get("Filter") match {
                      case Some(PdfName("Standard")) => println("Standard encryption")
                        dict.entries.get("R") match {
                          case Some(PdfNumber(n)) if n.toInt == 2 =>
                            println("128-bit encryption")
                            Success(false)
                          case Some(PdfNumber(n)) if n.toInt == 3 =>
                            println("256-bit encryption")
                            Success(false)
                          case Some(PdfNumber(n)) if n.toInt == 4 =>
                            println("Password-based encryption")
                            Success(true)
                          case unknown =>
                            println(s"Unknown encryption type: $unknown")
                            Success(false)
                        }
                      case _ =>
                        Success(false)
                    }

                  case Failure(e) =>
                    e.printStackTrace()
                    Success(false)
                }
            }
          case Success(_) => Success(false)
          case f@Failure(_) => f.asInstanceOf[Failure[Boolean]]
        }
      }.getOrElse(Success(false))
    } finally {
      file.close()
    }
  }

  def searchBackwardsForSequence(file: RandomAccessFile, sequence: Array[Byte], bufferSize: Int = 4096): Option[Long] = {
    val fileLength = file.length()
    val buffer = new Array[Byte](bufferSize)

    @tailrec
    def search(offset: Long, previousChunk: Array[Byte] = Array()): Option[Long] = {
      if (offset < 0) None
      else {
        val readSize = Math.min(bufferSize, offset + 1).toInt
        file.seek(offset - readSize + 1)
        file.readFully(buffer, 0, readSize)
        val chunk = buffer.take(readSize)

        val combinedChunk = chunk ++ previousChunk
        val index = findSequenceInChunk(combinedChunk, sequence)

        if (index >= 0) Some(offset - readSize + 1 + index)
        else search(offset - bufferSize, chunk.take(sequence.length - 1))
      }
    }

    search(fileLength - 1)
  }

  def findSequenceInChunk(chunk: Array[Byte], sequence: Array[Byte]): Int = {
    @tailrec
    def loop(i: Int): Int = {
      if (i < 0) -1
      else if (chunk.slice(i, i + sequence.length).sameElements(sequence)) i
      else loop(i - 1)
    }

    loop(chunk.length - sequence.length)
  }

  def readPdfObject(file: RandomAccessFile, pos: Long): String = {
    val startPattern = "<<".getBytes("UTF-8")
    val endPattern = ">>".getBytes("UTF-8")

    // Helper function to search for a byte pattern in the file
    @tailrec
    def searchPattern(position: Long, pattern: Array[Byte]): Long = {
      if (position >= file.length()) -1
      else {
        file.seek(position)
        val byte = file.readByte()
        if (byte == pattern(0)) {
          file.seek(position)
          val buffer = new Array[Byte](pattern.length)
          file.readFully(buffer)
          if (buffer.sameElements(pattern)) position
          else searchPattern(position + 1, pattern)
        } else {
          searchPattern(position + 1, pattern)
        }
      }
    }

    // Helper function to read the object until the matching ">>"
    def readObject(position: Long): String = {
      val buffer = new StringBuilder
      var currentPos = position
      var openCount = 0

      while (currentPos < file.length() && openCount >= 0) {
        file.seek(currentPos)
        val byte = file.readByte()
        buffer.append(byte.toChar)

        // Check for "<<" and ">>"
        if (byte == '<' && currentPos + 1 < file.length()) {
          file.seek(currentPos + 1)
          if (file.readByte() == '<') {
            buffer.append('<')
            openCount += 1
            currentPos += 1
          }
        } else if (byte == '>' && currentPos + 1 < file.length()) {
          file.seek(currentPos + 1)
          if (file.readByte() == '>') {
            buffer.append('>')
            openCount -= 1
            currentPos += 1
            if (openCount == 0) {
              return buffer.toString()
            }
          }
        }
        currentPos += 1
      }

      buffer.toString()
    }

    // Start searching from the beginning of the file
    file.seek(pos)
    val startPos = searchPattern(pos, startPattern)
    if (startPos == -1) {
      throw new IllegalArgumentException("No opening '<<' found")
    }

    // Read the object starting from the found position
    println(s"startPos = $startPos")
    readObject(startPos)
  }
}
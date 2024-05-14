import java.io.{File, RandomAccessFile}
import scala.util.Try
import scala.util.matching.Regex

// implements what's described here: https://stackoverflow.com/questions/35594240/how-to-check-if-pdf-is-password-protected-using-static-tools
/*
So to detect PDF Encryption, go to the end of the file and search upwards for the first line 
containing just the word 'trailer', then search downwards again for the string '/Encrypt'. 
If it's there, the file is encrypted, otherwise not.

Now, detecting whether a PDF is Password-protected, meaning so that you can't open it without 
supplying a password, is going to be harder. You basically need to read the Object reference 
after the /Encrypt key (e.g. '14 0 R'), jump to the beginning of the file and search for that 
object (e.g. '14 0 obj <<') and look for the /Filter , /R , and /U keys in that dictionary.

if the /Filter value is /Standard, then Per the preamble to Algorithm 3.6 'Authenticating the 
user password' (page 127), algorithm 3.6 can be used to determined whether the user password 
is the empty string and therefore whether to suppress prompting for a password.

So basically, if the /R value is 2, you will look for the /U value to be a specific string, 
and if the /R value is 3 or 4, you can look for the /U value to be another specific string, 
and if it is neither of those values then a user password is required to open the document and 
the document is password protected.

You could calculate those /U values by following the algorithms in the PDF Reference, or you can 
dig them out of existing encrypted PDFs that don't require a password to be opened.
 */
/*
object PdfPasswordChecker {

  val trailerRegex: Regex = """trailer""".r
  val encryptRegex: Regex = """/Encrypt (\d+) (\d+) R""".r
  val objRegex: Regex = """(\d+) (\d+) obj""".r
  val filterRegex: Regex = """/Filter /Standard""".r
  val rValueRegex: Regex = """/R (\d+)""".r
  val uValueRegex: Regex = """/U (\[.+])""".r

  def searchBackwardsForTrailer(file: RandomAccessFile): Option[Long] = {
    file.seek(file.length - 1)
    searchByteSequenceBackwards(file, "trailer".getBytes("ISO-8859-1"))
  }

  def findEncryptObjectId(trailerContent: String): Option[String] = {
    encryptRegex.findFirstMatchIn(trailerContent).map(m => s"${m.group(1)} ${m.group(2)} R")
  }

  def fetchObject(file: RandomAccessFile, objId: String): Option[String] = {
    file.seek(0) // Reset to start of the file
    searchByteSequenceForward(file, objId.getBytes("ISO-8859-1")).map(_ => objId)
  }

  def searchByteSequenceForward(file: RandomAccessFile, targetSequence: Array[Byte]): Option[Long] = {
    val bufferSize = 4096 // A reasonable buffer size, can be tuned based on expected data patterns
    val buffer = new Array[Byte](bufferSize)
    var overlap = new Array[Byte](targetSequence.length - 1)
    var bytesRead = 0

    while ( {
      bytesRead = file.read(buffer); bytesRead != -1
    }) {
      // Check for the sequence spanning between the end of the last buffer and the start of this one
      if ((overlap ++ buffer.slice(0, bytesRead)).containsSlice(targetSequence)) {
        return Some(file.getFilePointer - bytesRead - overlap.length + (overlap ++ buffer.slice(0, bytesRead)).indexOfSlice(targetSequence))
      }

      // Update overlap for next iteration
      overlap = buffer.slice(bytesRead - overlap.length, bytesRead)

      // Check within the current buffer
      for (i <- 0 until bytesRead - targetSequence.length + 1) {
        var found = true
        for (j <- targetSequence.indices if found) {
          if (buffer(i + j) != targetSequence(j)) found = false
        }
        if (found) return Some(file.getFilePointer - bytesRead + i)
      }
    }

    None
  }

  def searchByteSequenceBackwards(file: RandomAccessFile, targetSequence: Array[Byte]): Option[Long] = {
    val bufferSize = 4096 // Buffer size, adjustable based on performance needs
    val buffer = new Array[Byte](bufferSize)
    var overlap = new Array[Byte](targetSequence.length - 1)
    var pos = file.length()
    var bytesRead = 0

    while (pos > 0) {
      val sizeToRead = Math.min(bufferSize, pos).toInt
      pos -= sizeToRead
      file.seek(pos)
      bytesRead = file.read(buffer, 0, sizeToRead)

      // Prepare content for checking by reversing the buffer + overlap
      val contentToCheck = (buffer.slice(0, bytesRead) ++ overlap).reverse
      val reversedTarget = targetSequence.reverse

      // Update overlap for next iteration
      overlap = buffer.slice(0, Math.min(targetSequence.length - 1, bytesRead)).reverse

      // Check within the current buffer
      for (i <- 0 until bytesRead - reversedTarget.length + 1) {
        var found = true
        for (j <- reversedTarget.indices if found) {
          if (contentToCheck(i + j) != reversedTarget(j)) found = false
        }
        if (found) return Some(pos + bytesRead - i - targetSequence.length)
      }
    }

    None
  }

  def checkIfPasswordProtected(file: RandomAccessFile): Boolean = {
    searchBackwardsForTrailer(file).flatMap { _ =>
      searchByteSequenceForward(file, "/Encrypt".getBytes("ISO-8859-1")).flatMap { objectId =>
        fetchObject(file, "").flatMap { objectContent =>
          for {
            filterMatch <- filterRegex.findFirstIn(objectContent)
            rValueMatch <- rValueRegex.findFirstMatchIn(objectContent)
            uValueMatch <- uValueRegex.findFirstMatchIn(objectContent)
          } yield {
            val rValue = rValueMatch.group(1).toInt
            val uValue = uValueMatch.group(1)
            val knownUValues = Map(
              2 -> "[Some Known U Value for R=2]",
              3 -> "[Some Known U Value for R=3]",
              4 -> "[Some Known U Value for R=4]"
            )
            knownUValues.get(rValue).contains(uValue)
          }
        }
      }
    }.getOrElse(false)
  }

  def main(args: Array[String]): Unit = {
    val filePath = "/Users/ebowman/Downloads/7413-Article Text-17721-1-10-20121210.pdf"
    val file = new RandomAccessFile(new File(filePath), "r")
    try {
      println(s"Is the PDF password protected? ${checkIfPasswordProtected(file)}")
    } finally {
      file.close()
    }
  }
}
*/
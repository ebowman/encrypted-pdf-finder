package ie.boboco.cqp.pdf

import java.io.RandomAccessFile

object FileUtils {

  def searchForSequence(file: RandomAccessFile, sequence: Array[Byte], bufferSize: Int = 8192): Long = {
    val buffer = new Array[Byte](bufferSize)
    val sequenceLength = sequence.length
    val lastPossibleStart = bufferSize - sequenceLength

    @annotation.tailrec
    def searchFromPosition(filePosition: Long, bufferOffset: Int): Long = {
      val bytesRead = file.read(buffer, bufferOffset, bufferSize - bufferOffset)
      if (bytesRead == -1) {
        return -1 // End of file reached
      }

      val totalBytesRead = bufferOffset + bytesRead
      var i = 0
      while (i <= totalBytesRead - sequenceLength) {
        if (buffer.slice(i, i + sequenceLength).sameElements(sequence)) {
          return filePosition + i
        }
        i += 1
      }

      // Copy the last `sequenceLength - 1` bytes to the beginning of the buffer for overlap handling
      System.arraycopy(buffer, totalBytesRead - (sequenceLength - 1), buffer, 0, sequenceLength - 1)

      searchFromPosition(filePosition + totalBytesRead - sequenceLength + 1, sequenceLength - 1)
    }

    file.seek(0)
    searchFromPosition(0, 0)
  }
}
package ie.boboco.cqp.pdf

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.{Failure, Success, Try}

class PdfDictionaryParserSpec extends AnyFlatSpec with Matchers {

  "PdfDictionaryParser" should "parse a PDF dictionary with various entries" in {
    val input = "<< /Type /Catalog /Pages 2 0 R /OpenAction [3 0 R /FitH 0] /PageLayout /OneColumn >>"

    val result: Try[PdfDictionary] = PdfDictionaryParser.parsePdfDictionary(input)

    result shouldBe a[Success[?]]
    val pdfDict = result.get

    pdfDict.entries should contain("Type" -> PdfName("Catalog"))
    pdfDict.entries should contain("Pages" -> PdfReference(2, 0))
    pdfDict.entries should contain("OpenAction" -> PdfArray(Seq(
      PdfReference(3, 0),
      PdfName("FitH"),
      PdfNumber(0)
    )))
    pdfDict.entries should contain("PageLayout" -> PdfName("OneColumn"))
  }

  it should "parse a PDF dictionary with different value types" in {
    val input = "<< /Size 106 /Root 68 0 R /Encrypt 105 0 R /Info 104 0 R /ID [<daa1850e2ddc76fc29e0264bcff1bba2> <daa1850e2ddc76fc29e0264bcff1bba2>] >>"

    val result = PdfDictionaryParser.parsePdfDictionary(input)

    result shouldBe a[Success[?]]
    val pdfDict = result.get

    pdfDict.entries should contain("Size" -> PdfNumber(106))
    pdfDict.entries should contain("Root" -> PdfReference(68, 0))
    pdfDict.entries should contain("Encrypt" -> PdfReference(105, 0))
    pdfDict.entries should contain("Info" -> PdfReference(104, 0))
    pdfDict.entries should contain("ID" -> PdfArray(Seq(
      PdfString("daa1850e2ddc76fc29e0264bcff1bba2"),
      PdfString("daa1850e2ddc76fc29e0264bcff1bba2")
    )))
  }

  it should "parse a PDF dictionary with boolean values" in {
    val input = "<< /Flag true /Visible false >>"

    val result = PdfDictionaryParser.parsePdfDictionary(input)

    result shouldBe a[Success[?]]
    val pdfDict = result.get

    pdfDict.entries should contain("Flag" -> PdfBoolean(true))
    pdfDict.entries should contain("Visible" -> PdfBoolean(false)
    )
  }

  it should "parse a PDF dictionary with literal strings" in {
    val input = "<< /Title (Hello World) /Author (John Doe) >>"

    val result = PdfDictionaryParser.parsePdfDictionary(input)

    result shouldBe a[Success[?]]
    val pdfDict = result.get

    pdfDict.entries should contain("Title" -> PdfString("Hello World")
    )
    pdfDict.entries should contain("Author" -> PdfString("John Doe")
    )
  }

  it should "parse a PDF dictionary with nested dictionaries" in {
    val input = "<< /Type /Catalog /Pages << /Count 10 /Kids [1 0 R 2 0 R] >> >>"

    val result = PdfDictionaryParser.parsePdfDictionary(input)

    result shouldBe a[Success[?]]
    val pdfDict = result.get

    val nestedDict = PdfDictionary(Map(
      "Count" -> PdfNumber(10),
      "Kids" -> PdfArray(Seq(
        PdfReference(1, 0),
        PdfReference(2, 0)
      ))
    ))

    pdfDict.entries should contain("Type" -> PdfName("Catalog")
    )
    pdfDict.entries should contain("Pages" -> nestedDict)
  }

  it should "parse a PDF dictionary with null values" in {
    val input = "<< /Optional null /Name /Value >>"

    val result = PdfDictionaryParser.parsePdfDictionary(input)

    result shouldBe a[Success[?]]
    val pdfDict = result.get

    pdfDict.entries should contain("Optional" -> PdfNull)
    pdfDict.entries should contain("Name" -> PdfName("Value")
    )
  }

  it should "parse a PDF dictionary with hexadecimal strings" in {
    val input = "<< /Checksum <4f6e652054776f205468726565> >>"

    val result = PdfDictionaryParser.parsePdfDictionary(input)

    result shouldBe a[Success[?]]
    val pdfDict = result.get

    pdfDict.entries should contain("Checksum" -> PdfString("4f6e652054776f205468726565")
    )
  }

  it should "parse a PDF dictionary with mixed arrays" in {
    val input = "<< /Items [true 123 /Name (String) <616263>] >>"

    val result = PdfDictionaryParser.parsePdfDictionary(input)

    result shouldBe a[Success[?]]
    val pdfDict = result.get

    pdfDict.entries should contain("Items" -> PdfArray(Seq(
      PdfBoolean(true),
      PdfNumber(123)
      ,
      PdfName("Name")
      ,
      PdfString("String")
      ,
      PdfString("616263")
    )))
  }

  it should "fail to parse invalid PDF dictionary input" in {
    val input = "<< /Size 106 /Root 68 0 R /Encrypt 105 0 R /Info 104 0 R /ID [<daa1850e2ddc76fc29e0264bcff1bba2> <daa1850e2ddc76fc29e0264bcff1bba2>]"

    val result = PdfDictionaryParser.parsePdfDictionary(input)

    result shouldBe a[Failure[?]]
  }

  it should "fail to parse a dictionary with missing key" in {
    val input = "<< /Size 106 /Root 68 0 R /Encrypt 105 0 R /Info 104 0 R [<daa1850e2ddc76fc29e0264bcff1bba2> <daa1850e2ddc76fc29e0264bcff1bba2>] >>"

    val result = PdfDictionaryParser.parsePdfDictionary(input)

    result shouldBe a[Failure[?]]
  }

  it should "fail to parse a dictionary with mismatched brackets" in {
    val input = "<< /Size 106 /Root 68 0 R /Encrypt 105 0 R /Info 104 0 R /ID [<daa1850e2ddc76fc29e0264bcff1bba2> <daa1850e2ddc76fc29e0264bcff1bba2> >>>"

    val result = PdfDictionaryParser.parsePdfDictionary(input)

    result shouldBe a[Failure[?]]
  }

  // Add more test cases as needed
  it should "parse even if the dictionary has more text after it" in {
    val input = "<< /Type /Catalog /Pages 2 0 R /OpenAction [3 0 R /FitH 0] /PageLayout /OneColumn >>\nblah blah blah"

    val result: Try[PdfDictionary] = PdfDictionaryParser.parsePdfDictionary(input)

    result shouldBe a[Success[?]]
    val pdfDict = result.get

    pdfDict.entries should contain("Type" -> PdfName("Catalog"))
    pdfDict.entries should contain("Pages" -> PdfReference(2, 0))
    pdfDict.entries should contain("OpenAction" -> PdfArray(Seq(
      PdfReference(3, 0),
      PdfName("FitH"),
      PdfNumber(0))))
    pdfDict.entries should contain("PageLayout" -> PdfName("OneColumn"))

  }
  it should "parse this dictionary" in {
    // val input = "<< /ID [<><3A6E5C7C205BFFE0F34FD114A656B169> ] /Root 47 0 R /Size 64 /Prev 114448 /Info 1 0 R >> \nstartxref\n116350\n%%EOF"
    val input = "<< /ID [<><3A6E5C7C205BFFE0F34FD114A656B169> ] /Root 47 0 R /Size 64 /Prev 114448 /Info 1 0 R >>"
    val result: Try[PdfDictionary] = PdfDictionaryParser.parsePdfDictionary(input)
    println(result)
  }
}

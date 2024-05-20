package ie.boboco.cqp.pdf

import scala.util.Try
import scala.util.parsing.combinator.*

sealed trait PdfValue

case class PdfDictionary(entries: Map[String, PdfValue]) extends PdfValue

case class PdfNumber(value: Double) extends PdfValue

case class PdfReference(objNum: Int, genNum: Int) extends PdfValue

case class PdfString(value: String) extends PdfValue

case class PdfArray(values: Seq[PdfValue]) extends PdfValue

case class PdfName(value: String) extends PdfValue

case class PdfBoolean(value: Boolean) extends PdfValue

case object PdfNull extends PdfValue


object PdfDictionaryParser extends RegexParsers {

  def parsePdfDictionary(input: String): Try[PdfDictionary] = {
    parseAll(pdfDictionaryWithTrailing, input) match {
      case Success(result, _) => scala.util.Success(result)
      case failure: NoSuccess => scala.util.Failure(new Exception(failure.msg))
    }
  }

  private def pdfDictionaryWithTrailing: Parser[PdfDictionary] =
    pdfDictionary <~ """(\s|.)*""".r

  private def pdfDictionary: Parser[PdfDictionary] =
    "<<" ~> rep(pdfEntry) <~ ">>" ^^ { entries => PdfDictionary(entries.toMap) }

  private def pdfEntry: Parser[(String, PdfValue)] =
    pdfName ~ pdfValue ^^ { case name ~ value => (name, value) }

  private def pdfName: Parser[String] =
    "/" ~> """[a-zA-Z0-9]+""".r

  private def pdfValue: Parser[PdfValue] =
    pdfBoolean | pdfNull | pdfReference | pdfNumber | pdfArray | pdfString | pdfNameValue | pdfDictionary

  private def pdfBoolean: Parser[PdfBoolean] =
    ("true" | "false") ^^ { bool => PdfBoolean(bool.toBoolean) }

  private def pdfNull: Parser[PdfNull.type] =
    "null" ^^ (_ => PdfNull)

  private def pdfReference: Parser[PdfReference] =
    pdfNumber ~ pdfNumber <~ "R" ^^ { case PdfNumber(objNum) ~ PdfNumber(genNum) => PdfReference(objNum.toInt, genNum.toInt) }

  private def pdfNumber: Parser[PdfNumber] =
    """[-+]?\d*\.?\d+|\d+""".r ^^ { num => PdfNumber(num.toDouble) }

  private def pdfArray: Parser[PdfArray] =
    "[" ~> rep(pdfValue) <~ "]" ^^ { values => PdfArray(values) }

  private def pdfString: Parser[PdfString] =
    ("<" ~> """[0-9a-fA-F]*""".r <~ ">" ^^ { str => PdfString(str) }) |
      ("(" ~> """[^)]+""".r <~ ")" ^^ { str => PdfString(str) })

  private def pdfNameValue: Parser[PdfName] =
    "/" ~> """[a-zA-Z0-9]+""".r ^^ { name => PdfName(name) }
}


name := "encrypted-pdf-finder"

version := "0.1"

scalaVersion := "3.4.2"

libraryDependencies ++= Seq(
  "org.apache.pdfbox" % "pdfbox" % "3.0.2",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.4.0",
  "org.scalatest" %% "scalatest" % "3.2.18" % Test,
  "org.scalacheck" %% "scalacheck" % "1.17.1" % Test,
  "org.scalacheck" %% "scalacheck" % "1.17.1" % Test,
  "org.scalatestplus" %% "scalacheck-1-17" % "3.2.18.0" % Test
)

// Scoverage settings
coverageEnabled := true
coverageMinimumStmtTotal := 100
coverageMinimumBranchTotal := 100
coverageFailOnMinimum := true
coverageHighlighting := true

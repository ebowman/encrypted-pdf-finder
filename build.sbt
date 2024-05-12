name := "encrypted-pdf-finder"

version := "0.1"

scalaVersion := "3.4.1"

libraryDependencies ++= Seq(
  "org.apache.pdfbox" % "pdfbox" % "2.0.31",
  "dev.zio" %% "zio" % "1.0.17"
)


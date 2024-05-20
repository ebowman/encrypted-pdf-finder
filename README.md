# Encrypted PDF Finder

## Overview

This project, **Encrypted PDF Finder**, provides a concurrent pipeline for processing PDF files within a directory structure. The main functionalities include traversing directories, filtering for PDF files, and identifying password-protected PDFs. The pipeline leverages concurrency to maximize throughput and minimize processing time.

IMO there are two interesting parts of this program:

1. The ```parallelFindPDFs``` method in the ```PdfFileWorkflow``` trait. This method uses a concurrent queue to traverse directories and enqueue PDF files for processing. It is a good example of how to traverse a file system concurrently. It's much, much faster than the ```Files.walkFileTree``` method in Java.
2. 
## Project Structure

The project consists of three main components:

1. **PasswordPdfPipelineApp** - The main application that initializes and runs the pipeline.
2. **ConcurrentQueuePipelining** - A trait that provides the core functionality for creating concurrent pipelines.
3. **PdfFileWorkflow** - A trait that defines the workflow for processing PDF files, including methods for traversing directories and identifying password-protected PDFs.

## Files

### 1. `PasswordPdfPipelineApp.scala`

This file contains the main application which sets up and runs the pipeline.

#### Key Components

- **Main Object**: Initializes the application and sets the logging level.
- **Pipeline Setup**: Defines the root directory, available processors, and sets up the pipeline stages.
- **Execution**: Runs the pipeline and prints the processing time.

#### Usage

To run the application, simply execute the `PasswordPdfPipelineApp` object. It will process PDF files starting from the specified root directory and print out the password-protected PDFs.

### 2. `ConcurrentQueuePipelining.scala`

This trait provides functionality to pipeline operations on data items concurrently using a queue-based approach.

#### Key Components

- **PipelineQueue**: An implicit class that extends `LinkedBlockingQueue` with methods for chaining operations using the `>>` operator.
- **PipelineItem**: An implicit class that allows individual items to be processed through the pipeline stages.
- **Concurrency Execution**: Method to execute a processing function concurrently across multiple threads.

#### Usage

To use this trait, include it in your class or object and define your pipeline stages using the `>>` operator. Example usage is provided within the trait documentation.

### 3. `PdfFileWorkflow.scala`

This trait provides methods for processing PDF files within a directory structure, focusing on traversing directories, filtering for PDF files, and identifying password-protected PDFs.

#### Key Components

- **enqueuePasswordProtectedPdfs**: Checks if a PDF file is password protected and enqueues it if true.
- **parallelFindPDFs**: Recursively searches for PDF files starting from a given directory and enqueues them.

#### Usage

Include this trait in your class or object and use the provided methods as stages in your pipeline.

## Example Usage

Below is an example usage of the pipeline setup within the main application.

```scala
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import ie.boboco.cqp.ConcurrentQueuePipelining
import ie.boboco.cqp.pdf.PdfFileWorkflow

object PasswordPdfPipelineApp extends App with ConcurrentQueuePipelining with PdfFileWorkflow {
  val rootDir = new File("/Users/ebowman/src")
  val coreCount = Runtime.getRuntime.availableProcessors()
  val encPdfQueue = rootDir >> parallelFindPDFs >> (enqueuePasswordProtectedPdfs, coreCount)

  Iterator.continually(encPdfQueue.take())
    .takeWhile(_.isDefined)
    .flatten
    .foreach(println)
}
```

This setup will start from the root directory, find all PDF files, check if they are password-protected, and print the password-protected ones.

## Dependencies

- **Java 8 or higher**
- **Scala 3.4.1**
- **Apache PDFBox 3.0.2** for PDF processing

## Building and Running

1. Ensure you have Java and Scala installed.
2. Clone the repository.
3. Compile the project using `sbt compile`.
4. Run the main application using `sbt run`.

## Contributing

Contributions are welcome! Please fork the repository and create a pull request with your changes.

## License

This project is licensed under the MIT License.

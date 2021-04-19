package com.quantemplate.capitaliq.common

import java.nio.file.Path
import java.nio.file.FileSystems
import scala.util.{Try, Using}
import scala.io.{Source, BufferedSource}

object IO:
  def pwd = System.getProperty("user.dir")

  def stdin[A](fn: (BufferedSource => A)) = Using(Source.stdin)(fn)

  def toPath(rawPath: String) =
    FileSystems.getDefault.getPath(rawPath).normalize

  def absolutePath(rawPath: String) = 
    toPath(rawPath).toAbsolutePath

  def readLines(path: Path) =
    read(path)(_.getLines.toVector)

  def readAll(path: Path) =
    read(path)(_.getLines.mkString("\n"))

  def read[A](path: Path): (BufferedSource => A) => Try[A] =
     Using(Source.fromFile(path.toFile))

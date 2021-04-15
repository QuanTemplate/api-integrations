package com.quantemplate.capitaliq.common

import java.nio.file.Paths

def getPath(rawPath: String, base: os.Path = os.pwd) =
  if Paths.get(rawPath).isAbsolute
    then os.Path(rawPath)
    else os.Path(rawPath, base = os.pwd)

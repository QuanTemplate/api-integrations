package com.quantemplate.capitaliq.qt

trait QTError extends Throwable

case class DatasetUploadError(res: String) extends QTError:
  override def toString = s"Could not upload the dataset to the Quantemplate.\nAPI Response: $res"

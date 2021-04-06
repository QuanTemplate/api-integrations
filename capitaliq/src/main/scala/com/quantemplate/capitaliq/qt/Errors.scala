package com.quantemplate.capitaliq.qt

trait QTError extends Throwable {
  def message: String
}
// todo: use show instance
case class DatasetUploadError(res: String) extends QTError {
  def message = s"Could not upload the dataset to the Quantemplate.\nAPI Response: $res"
}
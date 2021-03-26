package com.quantemplate.capitaliq.domain

trait DomainError extends Throwable {
  def message: String
}

case class InvalidServiceParametersError(msg: String) extends DomainError:
  def message = s"Invalid Capital IQ API params: $msg"

case class UnexpectedEmptyRows(msg: String) extends DomainError:
  def message = s"Capital IQ API has returned no rows: $msg"

case class UnrecognizedServiceError(msg: String) extends DomainError:
  def message = s"Unrecognized Capital IQ API error: $msg"

case class MnemonicsError(errors: Seq[DomainError]) extends DomainError:
  def message = errors.map(_.message).mkString
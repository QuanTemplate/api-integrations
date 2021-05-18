package com.quantemplate.integrations.capitaliq

trait DomainError extends Throwable

case class InvalidServiceParametersError(msg: String) extends DomainError:
  override def toString = s"Invalid Capital IQ API params: $msg"

case class UnexpectedEmptyRows(msg: String) extends DomainError:
  override def toString = s"Capital IQ API has returned no rows: $msg"

case class UnrecognizedServiceError(msg: String) extends DomainError:
  override def toString = s"Unrecognized Capital IQ API error: $msg"

case class MnemonicsError(errors: Seq[DomainError]) extends DomainError:
  override def toString = errors.mkString(",")

case object DailyMnemonicLimitReachedError extends DomainError:
  override def toString = s"Reached daily limit of 24000 requested mnemonics"
  
package com.quantemplate.integrations.qt

import com.quantemplate.integrations.common.HttpService

trait QTError extends Throwable

case class Forbidden(action: String, res: HttpService.Response) extends QTError:
  override def toString = 
    s"""You are not authorized to perform $action.
       |Check credentials of your QT api-user and make sure it is created for the right org
       |If you are accessing dataset remember to share it with whole org with `edit` permissions
       |Response: $res""".stripMargin

case class UnexpectedError(res: HttpService.Response) extends QTError:
  override def toString = s"Unexpected error: $res"

case class NotFound(action: String, res: HttpService.Response)  extends QTError:
  override def toString =
    s"""Not found: $action
      |"Response: $res""".stripMargin

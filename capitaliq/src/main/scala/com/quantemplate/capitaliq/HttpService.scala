package com.quantemplate.capitaliq

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{RequestEntity, HttpRequest, HttpMethod, HttpMethods, HttpResponse, HttpEntity}
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.model.headers.Authorization
import akka.util.ByteString
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport.*
import io.circe.{Encoder, Decoder}

class HttpService(using system: ActorSystem[_]):
  given ExecutionContext = system.executionContext

  def POST[A: Encoder, B: Decoder](
    endpoint: String, 
    req: A, 
    auth: Option[Authorization] = None
  ): Future[B] =
    for 
      entity <- Marshal(req).to[RequestEntity]
      request = HttpRequest(
        method = HttpMethods.POST,
        uri = endpoint,
        entity = entity,
        headers = auth.map(Seq(_)).getOrElse(Seq.empty)
      )
      res <- Http().singleRequest(request)
      str <- res.entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
      result <- Unmarshal(str).to[B]
    yield result

  def POST2[B: Decoder](
    endpoint: String, 
    entity: RequestEntity,
    auth: Option[Authorization] = None
  ): Future[B] =
    val request = HttpRequest(
        method = HttpMethods.POST,
        uri = endpoint,
        entity = entity,
        headers = auth.map(Seq(_)).getOrElse(Seq.empty)
      )

    for 
      res <- Http().singleRequest(request)
      str <- res.entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
      result <- Unmarshal(str).to[B]
    yield result


  def upload[A <: java.io.ByteArrayOutputStream](endpoint: String, req: A, auth: Option[Authorization] = None): Future[ByteString] = 
    val bytes = req.toByteArray()
    val request = HttpRequest(
        method = HttpMethods.POST,
        uri = endpoint,
        entity = HttpEntity(bytes),
        headers = auth.map(Seq(_)).getOrElse(Seq.empty)
      )

    for 
     
      res <- Http().singleRequest(request)
      str <- res.entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
      // result <- Unmarshal(str).to[B]
      _ = println(res.status) // 403 :thinking
      _ = println(str.utf8String) 

      //  not a valid key=value pair (missing equal-sign) in Authorization header: 'Bearer ... '
      // {"message":"'eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJjc2lmaVg3eHZ5d1R0SHB2RWNaUHo5czZnNTJQMW10MkpLUENRRnBYanI4In0.eyJleHAiOjE2MTcyODk4MjMsImlhdCI6MTYxNzI4OTUyMywianRpIjoiNTQ5YzUyMTMtYzA4NC00ZTdlLWEyNDMtNGRmZTEwNDA4YTczIiwiaXNzIjoiaHR0cHM6Ly9hY2NvdW50cy5kZXYucXVhbnRlbXBsYXRlLmNvbS9hdXRoL3JlYWxtcy90ZXN0IiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImIwMzM2NzQxLWQ4ODAtNDI3Mi04NWJmLWNmMDI3NDczMjYxZiIsInR5cCI6IkJlYXJlciIsImF6cCI6InUtY2FwaXRhbC1pbmdyZXMtY2xpZW50dTQ0Iiwic2Vzc2lvbl9zdGF0ZSI6IjJlMmE3MmQ5LTM3N2YtNDllNC1iNWU5LTFkM2JiOWU0MWZmYSIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoicHJvZmlsZSBlbWFpbCIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwiY2xpZW50SG9zdCI6Ijc3LjI1Mi42MS4xMDgiLCJvcmdhbmlzYXRpb25JZCI6ImMtbXktc21hbGwtaW5zdXJhbmMtbHRkemZkIiwiY2xpZW50SWQiOiJ1LWNhcGl0YWwtaW5ncmVzLWNsaWVudHU0NCIsImFwaUtleSI6IjE1MjE0ZTI1LTVjZmYtNDRmNy04MDIxLWIyNDVhYzNlYmNhNiIsInByZWZlcnJlZF91c2VybmFtZSI6InNlcnZpY2UtYWNjb3VudC11LWNhcGl0YWwtaW5ncmVzLWNsaWVudHU0NCIsInVzZXJJZCI6InUtY2FwaXRhbC1pbmdyZXMtY2xpZW50dTQ0IiwiY2xpZW50QWRkcmVzcyI6Ijc3LjI1Mi42MS4xMDgifQ.unk_WJYqOlgWXw_8m3tCBthKxNM4LVsI1ntQ43NPcYQAxcsEUVu_Z4oaio5yvaeEClbRV8OTHZMbIuKkdWI21NsAJd1ljTJEfgg_zxNjnjzRgOJEtB7oMb6iMBE8nmFzd3HgNB6HJNl-aLvAgmPT6fRi9P7BZzuTMnnz761PhrKHB-nRJhulM-Wzm22PL6iC6ltxTmDn-5TD7mWF-zyfEoGwGZgHl9wndmb8Oh-7et-0tWVy1_TSpX9BGuFn-VoUH873ht-KiupaRbfAx_qvc4PVjUl2zvzTlKMWUmR75OpMRiucgT2QKlTgUzSrKEcEW7MkN1LuzVQguqraVYWx5g' not a valid key=value pair (missing equal-sign) in Authorization header: 'Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJjc2lmaVg3eHZ5d1R0SHB2RWNaUHo5czZnNTJQMW10MkpLUENRRnBYanI4In0.eyJleHAiOjE2MTcyODk4MjMsImlhdCI6MTYxNzI4OTUyMywianRpIjoiNTQ5YzUyMTMtYzA4NC00ZTdlLWEyNDMtNGRmZTEwNDA4YTczIiwiaXNzIjoiaHR0cHM6Ly9hY2NvdW50cy5kZXYucXVhbnRlbXBsYXRlLmNvbS9hdXRoL3JlYWxtcy90ZXN0IiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImIwMzM2NzQxLWQ4ODAtNDI3Mi04NWJmLWNmMDI3NDczMjYxZiIsInR5cCI6IkJlYXJlciIsImF6cCI6InUtY2FwaXRhbC1pbmdyZXMtY2xpZW50dTQ0Iiwic2Vzc2lvbl9zdGF0ZSI6IjJlMmE3MmQ5LTM3N2YtNDllNC1iNWU5LTFkM2JiOWU0MWZmYSIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoicHJvZmlsZSBlbWFpbCIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwiY2xpZW50SG9zdCI6Ijc3LjI1Mi42MS4xMDgiLCJvcmdhbmlzYXRpb25JZCI6ImMtbXktc21hbGwtaW5zdXJhbmMtbHRkemZkIiwiY2xpZW50SWQiOiJ1LWNhcGl0YWwtaW5ncmVzLWNsaWVudHU0NCIsImFwaUtleSI6IjE1MjE0ZTI1LTVjZmYtNDRmNy04MDIxLWIyNDVhYzNlYmNhNiIsInByZWZlcnJlZF91c2VybmFtZSI6InNlcnZpY2UtYWNjb3VudC11LWNhcGl0YWwtaW5ncmVzLWNsaWVudHU0NCIsInVzZXJJZCI6InUtY2FwaXRhbC1pbmdyZXMtY2xpZW50dTQ0IiwiY2xpZW50QWRkcmVzcyI6Ijc3LjI1Mi42MS4xMDgifQ.unk_WJYqOlgWXw_8m3tCBthKxNM4LVsI1ntQ43NPcYQAxcsEUVu_Z4oaio5yvaeEClbRV8OTHZMbIuKkdWI21NsAJd1ljTJEfgg_zxNjnjzRgOJEtB7oMb6iMBE8nmFzd3HgNB6HJNl-aLvAgmPT6fRi9P7BZzuTMnnz761PhrKHB-nRJhulM-Wzm22PL6iC6ltxTmDn-5TD7mWF-zyfEoGwGZgHl9wndmb8Oh-7et-0tWVy1_TSpX9BGuFn-VoUH873ht-KiupaRbfAx_qvc4PVjUl2zvzTlKMWUmR75OpMRiucgT2QKlTgUzSrKEcEW7MkN1LuzVQguqraVYWx5g'."}

    yield str

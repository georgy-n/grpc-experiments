package ru.meetup.grpc

import io.grpc._
import io.grpc.stub.MetadataUtils

object ClientHeaderInterceptor {

  def withHeaders(headers: Map[String, String]): ClientInterceptor = {
    val meta: Metadata = new Metadata
    headers.foreach {
      case (key, value) => meta.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value)
    }
    MetadataUtils.newAttachHeadersInterceptor(meta)
  }
}


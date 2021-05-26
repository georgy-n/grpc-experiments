package ru.meetup.grpc.interceptor



import com.typesafe.scalalogging.Logger
import io.grpc.{Context, Metadata, ServerStreamTracer}

import scala.collection.JavaConverters._


object StreamFactory {

  def streamFactory(logger: Logger): ServerStreamTracer.Factory =
    (fullMethodName: String, headers: Metadata) =>
      new ServerStreamTracer {
        override def filterContext(context: Context): Context = {
          logger.info(s"received $fullMethodName with headers $headers")
          Context
            .current
            .withValue(StreamFactory.grpcHeadersKey, StreamFactory.pullOutHeadersMap(headers))
        }
      }

    private val grpcHeadersName: String = "grpc-headers"
    private val grpcHeadersKey: Context.Key[Map[String, String]] = Context.key(grpcHeadersName)

    def grpcHeaders: Map[String, String] = grpcHeadersKey.get()

    private def pullOutHeadersMap(requestHeaders: Metadata): Map[String, String] =
      requestHeaders.keys().asScala.collect {
        case key if !key.endsWith(Metadata.BINARY_HEADER_SUFFIX) =>
          key.toUpperCase() ->
            requestHeaders
              .get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER))
      }.toMap
}

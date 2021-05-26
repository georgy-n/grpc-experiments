package ru.meetup.grpc

import com.typesafe.scalalogging.LazyLogging
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener
import io.grpc._
import scalapb.GeneratedMessage

class ClientLoggingInterceptor extends ClientInterceptor with LazyLogging {
  override def interceptCall[ReqT, RespT](
                                           method: MethodDescriptor[ReqT, RespT],
                                           callOptions: CallOptions,
                                           next: Channel
                                         ): ClientCall[ReqT, RespT] = {

    val exampleValue  = callOptions.getOption(CallOptionKeys.ExampleKey)
    val destination = method.getFullMethodName

    logger.info(s"getting exampleValue from callOptions $exampleValue, and destination $destination")

    new ForwardingClientCall.SimpleForwardingClientCall[ReqT, RespT](next.newCall(method, callOptions)) {

      override def sendMessage(message: ReqT): Unit = {
        logger.info(
          s"gRPC request with message ${getMessageContent(message)}"
        )
        super.sendMessage(message)
      }

      override def start(responseListener: ClientCall.Listener[RespT], headers: Metadata): Unit = {
        val listener = new SimpleForwardingClientCallListener[RespT](responseListener) {

          override def onMessage(message: RespT): Unit = {

            logger.info(s"gRPC response with response ${getMessageContent(message)}")
            super.onMessage(message)
          }

          override def onClose(status: Status, trailers: Metadata): Unit = {
            val listResponseMeta = ExtractResponseMeta.fromStatusAndTrailers(status, trailers).getOrElse(Nil)
            val trackingIds = listResponseMeta.flatMap(responseMeta => responseMeta.trackingId)

            if (status.isOk) {
              logger.info(
                s"gRPC completed, metaInformation -> ${listResponseMeta.map(getMessageContent)}"
              )
            } else {
              logger.info(
                s"gRPC failed, metaInformation -> ${listResponseMeta.map(getMessageContent)}, grpcExternalError -> ${status.getCode}",
                status.asException
              )
            }

            super.onClose(status, trailers)
          }
        }
        super.start(listener, headers)
      }
    }
  }

  private def getMessageContent(message: Any): String =
    message match {
      case generated: GeneratedMessage =>
        generated.toProtoString
      case _ => ""
    }
}
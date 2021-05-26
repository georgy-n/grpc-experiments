package ru.meetup.grpc

import io.grpc.{Metadata, Status}
import io.grpc.protobuf.StatusProto
import com.google.rpc.{Status => RpcStatus}

import scala.collection.convert.ImplicitConversions._

object ExtractResponseMeta {

  def fromStatusAndTrailers(status: Status, trailers: Metadata): Option[List[ResponseMeta]] = {
    val rpcStatus = Option(StatusProto.fromThrowable(status.asException(trailers)))
    fromRpcStatus(rpcStatus)
  }


  def fromRpcStatus(status: Option[RpcStatus]): Option[List[ResponseMeta]] = {
    val listCodeInputs   = status.map(_.getDetailsList.toList.map(detail => detail.getValue.newCodedInput()))
    listCodeInputs.map(_.map(codeInput => ResponseMeta.parseFrom(codeInput)))
  }
}

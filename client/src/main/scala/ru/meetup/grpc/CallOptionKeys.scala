package ru.meetup.grpc

import io.grpc.CallOptions

object CallOptionKeys {
  val ExampleKey: CallOptions.Key[String] = CallOptions.Key.create(CallHeaders.ExampleHeader)
}

object CallHeaders {
  val ExampleHeader: String = "ExampleHeader"
}
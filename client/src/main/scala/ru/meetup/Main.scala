package ru.meetup

import io.grpc.ManagedChannel
import ru.meetup.grpc.dto.UserId
import ru.meetup.grpc.{CallOptionKeys, ClientHeaderInterceptor, ClientLoggingInterceptor}
import ru.meetup.grpc.dto.service.accounts.{AccountServiceGrpc, GetAccountsRequest}
import cats.syntax.all._
import io.grpc.netty.shaded.io.grpc.netty.{NegotiationType, NettyChannelBuilder}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App {

  val channel: ManagedChannel =
    NettyChannelBuilder
      .forAddress("localhost", 9999)
      .negotiationType(NegotiationType.valueOf("PLAINTEXT"))
      .userAgent("meetup-service")
      .build()


  val r = AccountServiceGrpc
    .stub(channel)
    .withOption(CallOptionKeys.ExampleKey, "exampleValue")
    .withInterceptors(new ClientLoggingInterceptor, ClientHeaderInterceptor.withHeaders(Map("foo" -> "bar")))
    .getAccounts(GetAccountsRequest(UserId("userId".some).some))

  Await.result(r, Duration.Inf)
}

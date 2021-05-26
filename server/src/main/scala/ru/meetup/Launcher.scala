package ru.meetup

import cats.effect.Resource
import io.grpc.{Server, ServerServiceDefinition}
import io.grpc.stub.ServerCalls
import ru.meetup.backend.{AccountBackendImpl, CardBackendImpl}
import ru.meetup.domain.{Account, CreditAccount, DebitAccount, DepositAccount}
import ru.meetup.grpc.dto.service.accounts.{AccountServiceGrpc, GetAccountsRequest, GetAccountsResponse}
import ru.meetup.grpc.unary.ZIOGrpcUnaryMethod
import ru.meetup.service.AccountServiceImpl
import zio.{ExitCode, URIO, ZEnv, ZIO, ZLayer}
import zio.interop.catz._
import ru.meetup.grpc.dto
import cats.syntax.all._
import com.typesafe.scalalogging.LazyLogging
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import ru.meetup.grpc.interceptor.StreamFactory
import zio.interop.catz._

import scala.concurrent.duration._

object Launcher extends zio.App with LazyLogging {

  def build: Resource[MeetupTask, Server] = {
    val runtime        = zio.Runtime.default
    val accountBackend = new AccountBackendImpl[MeetupTask]
    val cardBackend    = new CardBackendImpl[MeetupTask]
    val service = new AccountServiceImpl[MeetupTask](accountBackend, cardBackend)
    val serverBuilder: ServerServiceDefinition.Builder = ServerServiceDefinition.builder(AccountServiceGrpc.SERVICE)

    val serv = for {
      env <- ZIO.environment[ZEnv]
      accountServiceDefinition = serverBuilder.addMethod(
        AccountServiceGrpc.METHOD_GET_ACCOUNTS,
        ServerCalls.asyncUnaryCall(new ZIOGrpcUnaryMethod[
          GetAccountsRequest,
          GetAccountsResponse,
          String,
          List[Account]
        ](
          runtime    = runtime,
          env = env,
          timeout    = _ => 10.seconds,
          methodName = AccountServiceGrpc.METHOD_GET_ACCOUNTS.getFullMethodName,
          func       = service.getAccounts
        )(
          _.userId.flatMap(_.value).toValidNec("empty userId"),
          list => GetAccountsResponse(list.map(toProto))
        ))
      ).build()
      server = NettyServerBuilder
        .forPort(9999)
        //TODO you know what you should do
        .executor(scala.concurrent.ExecutionContext.global)
        .addStreamTracerFactory(StreamFactory.streamFactory(logger))
        .addService(accountServiceDefinition)
        .addService(ProtoReflectionService.newInstance())
        .build()
    } yield server

    Resource.make[MeetupTask, Server](serv.map(_.start()))(serv => ZIO.effect(serv.shutdown()))
  }

  // TODO correct transformation
  def toProto(domain: Account): dto.Account = domain match {
    case CreditAccount(id, name, userId, amount, status, creditLimit, cards) =>
      dto.Account(dto.Account.Value.Credit(dto.Credit(
        id = dto.AccountId(id.some).some,
        name = name.some,
        status = dto.AccountStatusCode.NORMAL,
        contactId = dto.UserId(userId.some).some,
        amount = none,
        creditLimit = none,
        cards = Seq()
      )))
    case DebitAccount(id, name, userId, amount, status, cards)               => dto.Account(dto.Account.Value.Debit(dto.Debit(
        id = dto.AccountId(id.some).some,
        name = name.some,
        status = dto.AccountStatusCode.NORMAL,
        contactId = dto.UserId(userId.some).some,
        amount = none,
        cards = Seq()
      )))
    case DepositAccount(id, name, userId, amount, status)                    => dto.Account(dto.Account.Value.Deposit(dto.Deposit(
        id = dto.AccountId(id.some).some,
        name = name.some,
        status = dto.AccountStatusCode.NORMAL,
        contactId = dto.UserId(userId.some).some,
        amount = none
      )))
  }

  def run(args: List[String]): URIO[ZEnv, ExitCode] = {
    build.use(_ => ZIO.never)
      .exitCode
      .provideLayer(ZLayer.requires[ZEnv])
  }

}

package ru.meetup.grpc.unary

import java.util.concurrent.{CompletionException, TimeUnit, TimeoutException}

import cats.data.{NonEmptyChain, ValidatedNec}
import cats.syntax.all._
import com.google.protobuf.any.{Any => ProtoAny}
import com.google.rpc.{Code, Status}
import com.typesafe.scalalogging.LazyLogging
import io.grpc.protobuf.StatusProto
import io.grpc.stub.{ServerCallStreamObserver, ServerCalls, StreamObserver}
import ru.meetup.MeetupTask
import ru.meetup.grpc.ResponseMeta
import ru.meetup.grpc.interceptor.StreamFactory
import zio.duration.Duration
import zio.{IO, ZEnv, _}

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NoStackTrace

// TODO use functional logging
class ZIOGrpcUnaryMethod[GRPCReq, GRPCResp, DomainReq, DomainResp](
  val runtime: Runtime[ZEnv],
  val env: ZEnv,
  val timeout: String => FiniteDuration,
  val methodName: String,
  val func: DomainReq => MeetupTask[DomainResp]
)(
  implicit
  fromP: GRPCReq => ValidatedNec[String, DomainReq],
  toP: DomainResp => GRPCResp
) extends ServerCalls.UnaryMethod[GRPCReq, GRPCResp] with LazyLogging {

  private case object ClientCanceled extends CompletionException("canceled from client side") with NoStackTrace
  private case object TO             extends TimeoutException("timeout in GRPC handler") with NoStackTrace

  final case class MissedRequiredArgsError(args: NonEmptyChain[String])
    extends Exception("Missed required arguments: " + args.reduceLeft(_ + ", " + _))

  override def invoke(request: GRPCReq, observer: StreamObserver[GRPCResp]): Unit = {
    val listener = observer.asInstanceOf[ServerCallStreamObserver[GRPCResp]]

    val canceledPromise = runtime.unsafeRun(
      for  {
        prom <- zio.Promise.make[ClientCanceled.type, GRPCResp]
        _ = listener
          .setOnCancelHandler(
            () => runtime.unsafeRunAsync_(prom.fail(ClientCanceled))
          )
      } yield prom
    )

    // we can use client timeout from headers and use it for ZIO.timeout
    val timeoutFromHeader              = Option(io.grpc.Context.current().getDeadline).map(_.timeRemaining(TimeUnit.NANOSECONDS))
    val defaultTimeout                 = timeout(methodName).toNanos
    val operationTimeout               = timeoutFromHeader.fold(Duration.Finite(defaultTimeout))(headerTimeout =>
      Duration.Finite(math.min(defaultTimeout, headerTimeout))
    )

    logger.info(s"headers ${StreamFactory.grpcHeaders}")

    // convert -> call -> convert
    val funcCall: MeetupTask[GRPCResp] = fromP(request)
      .fold(
        missed => Task.fail(MissedRequiredArgsError(missed)),
        args => IO.effect(logger.info("start computing")) *> func(args).map(x => toP(x)) <* IO.effect(logger.info("end computing"))
      )

    val mainChain: MeetupTask[GRPCResp] = for {
      res <- funcCall.timeoutFail(TO)(operationTimeout)
    } yield res

    runtime.unsafeRunAsync(
      canceledPromise.await.either.race(mainChain.either).timed
        .provideLayer(ZLayer.requires[ZEnv])
      .catchAllCause(th => ZIO.fail(th.squash))
    )  {
       exit: Exit[Throwable, (Duration, Either[Throwable, GRPCResp])] =>
        exit.fold(
          // если все плохо, произошла критическая и непонятная ошибка
          cause =>
            cause.dieOption.fold({
              observer.onError(
                StatusProto.toStatusRuntimeException(
                  Status
                    .newBuilder()
                    .setCode(Code.INTERNAL.getNumber)
                    .build()
                )
              )
            })({ err =>
              observer.onError(
                StatusProto.toStatusRuntimeException(
                  Status
                    .newBuilder()
                    .setCode(Code.INTERNAL.getNumber)
                    .setMessage(renderException(err))
                    .build()
                )
              )
            }),
          {
            //  клиент перестал ждать
            case ((_, Left(_@ ClientCanceled))) =>
              logger.warn("client canceled request")

            case ((duration, Left(err))) =>
              observer.onError(
                StatusProto.toStatusRuntimeException(
                  Status
                    .newBuilder()
                    .setCode(Code.INTERNAL.getNumber)
                    .setMessage(renderException(err))
                    .addDetails(ProtoAny.toJavaProto(ProtoAny.pack(renderResponseMeta())))
                    .build()
                )
              )

            case ((duration, Right(payload))) =>
              observer.onNext(payload)
              logger.warn(s"completed successfully with time $duration")

              // onCompleted === onError with Code.OK
              observer.onError(
                StatusProto.toStatusRuntimeException(
                  Status
                    .newBuilder()
                    .setCode(Code.OK.getNumber)
                    .addDetails(ProtoAny.toJavaProto(ProtoAny.pack(renderResponseMeta())))
                    .build()
                )
              )
          }
        )
    }
  }

  private def renderException(ex: Throwable): String = ex.getMessage

  private def renderResponseMeta(): ResponseMeta =
    ResponseMeta("stub".some, List("fake warning"))
}

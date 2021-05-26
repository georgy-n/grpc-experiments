package ru.meetup.backend

import cats.effect.Sync
import ru.meetup.backend.AccountBackend.{AccountRaw, CreditAccountRaw}

trait AccountBackend[F[_]] {
  def getAccounts(userId: String): F[List[AccountRaw]]
}

class AccountBackendImpl[F[_]: Sync] extends AccountBackend[F] {
  override def getAccounts(userId: String): F[List[AccountRaw]] =
    Sync[F].delay(List(CreditAccountRaw("a", "b", "c", 123, "RUB", "NEW", 123)))
}
object AccountBackend {

  sealed trait AccountRaw {
    def id: String
    def name: String
    def userId: String
  }

  final case class CreditAccountRaw(
    id: String,
    name: String,
    userId: String,
    amount: BigDecimal,
    currency: String,
    status: String,
    creditLimit: BigDecimal
  ) extends AccountRaw

  final case class DebitAccountRaw(
    id: String,
    name: String,
    userId: String,
    amount: BigDecimal,
    currency: String,
    status: String
  ) extends AccountRaw

  final case class DepositAccountRaw(
    id: String,
    name: String,
    userId: String,
    amount: BigDecimal,
    currency: String,
    status: String
  ) extends AccountRaw

}

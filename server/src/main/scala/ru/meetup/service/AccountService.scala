package ru.meetup.service

import cats.effect.{Async, Sync}
import cats.syntax.all._
import com.typesafe.scalalogging.LazyLogging
import ru.meetup.backend.AccountBackend.AccountRaw
import ru.meetup.backend.CardBackend.CardRaw
import ru.meetup.backend.{AccountBackend, CardBackend}
import ru.meetup.domain.{Account, AccountStatus, Card, CreditAccount, Currency, DebitAccount, DepositAccount, MoneyAmount, PaymentSystem}
import ru.meetup.service.AccountServiceImpl.verySlowOperation

trait AccountService[F[_]] {
  def getAccounts(userId: String): F[List[Account]]
}

class AccountServiceImpl[F[_]: Sync](
  accountBackend: AccountBackend[F],
  cardBackend: CardBackend[F]
) extends AccountService[F] with LazyLogging {

  override def getAccounts(userId: String): F[List[Account]] = for {
    accs  <- accountBackend.getAccounts(userId)
    _ = logger.info(s"getting accounts $accs")
    _ <- Sync[F].delay(verySlowOperation)
    cards <- cardBackend.getCards(accs.map(_.id))
    _ = logger.info(s"getting cards $cards")
  } yield accs.map(acc => AccountServiceImpl.transformAccounts(acc, cards.getOrElse(acc.id, List.empty)))

}

object AccountServiceImpl {

  def verySlowOperation: Int = {
    for {
      i <- 1 to 10000
      j <- 1 to 1000
      r = math.sqrt(i * j)
    } yield 1
    1
  }

  def transformCards(cardRaw: CardRaw): Card = Card(
    id = cardRaw.id,
    cardNumber = cardRaw.cardNumber,
    paymentSystem = PaymentSystem.withNameInsensitive(cardRaw.paymentSystem)
  )

  def transformAccounts(accountRaw: AccountRaw, cards: List[CardRaw]): Account = accountRaw match {
    case AccountBackend.CreditAccountRaw(id, name, userId, amount, currency, status, creditLimit) => CreditAccount(
        id,
        name,
        userId,
        MoneyAmount(amount, Currency(currency)),
        AccountStatus.withNameInsensitive(status),
        MoneyAmount(creditLimit, Currency(currency)),
        cards.map(transformCards)
      )
    case AccountBackend.DebitAccountRaw(id, name, userId, amount, currency, status)               => DebitAccount(
        id,
        name,
        userId,
        MoneyAmount(amount, Currency(currency)),
        AccountStatus.withNameInsensitive(status),
        cards.map(transformCards)
      )
    case AccountBackend.DepositAccountRaw(id, name, userId, amount, currency, status)             => DepositAccount(
        id,
        name,
        userId,
        MoneyAmount(amount, Currency(currency)),
        AccountStatus.withNameInsensitive(status)
      )
  }

}

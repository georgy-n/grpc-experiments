package ru.meetup.domain

import java.time.Instant

sealed trait Account {
  def userId: String
  def name: String
  def marketingName: Option[String]
  def creationDate: Option[Instant]
  def currency: Currency
}

final case class Currency(
  code: String
)

final case class CreditAccount(
  userId: String,
  name: String,
  marketingName: Option[String],
  creationDate: Option[Instant],
  currency: Currency,

  lastPaymentDate: Instant,
  lastStatementDebt: MoneyAmount,
  cards: List[Card]
) extends Account

final case class DebitAccount(
  userId: String,
  name: String,
  marketingName: Option[String],
  creationDate: Option[Instant],
  currency: Currency,

  debtBalance: MoneyAmount,
  feeAndFinesBalance: MoneyAmount,
  rate: BigDecimal,
  cards: List[Card]
) extends Account

final case class Card(
  userId: String,
  ucid: String,
  status: CardStatus,
  balance: MoneyAmount,
  creationDate: Instant
)

sealed trait CardStatus
case object Norm extends CardStatus
case object Closed extends CardStatus

final case class MoneyAmount(
  amount: BigDecimal,
  currency: Currency
)
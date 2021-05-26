package ru.meetup.domain

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed trait Account {
  def id: String
  def name: String
  def userId: String
}

final case class Currency(
  code: String
)

final case class CreditAccount(
  id: String,
  name: String,
  userId: String,
  amount: MoneyAmount,
  status: AccountStatus,
  creditLimit: MoneyAmount,
  cards: List[Card]
) extends Account

final case class DebitAccount(
  id: String,
  name: String,
  userId: String,
  amount: MoneyAmount,
  status: AccountStatus,
  cards: List[Card]
) extends Account

final case class DepositAccount(
  id: String,
  name: String,
  userId: String,
  amount: MoneyAmount,
  status: AccountStatus
) extends Account

final case class Card(
  id: String,
  cardNumber: String,
  paymentSystem: PaymentSystem
)

sealed trait AccountStatus extends EnumEntry

object AccountStatus extends Enum[AccountStatus] {

  case object Norm   extends AccountStatus
  case object New    extends AccountStatus
  case object Closed extends AccountStatus

  override def values: immutable.IndexedSeq[AccountStatus] = findValues
}

sealed trait CardStatus extends EnumEntry

object CardStatus extends Enum[CardStatus] {

  case object Norm   extends CardStatus
  case object Closed extends CardStatus

  override def values: immutable.IndexedSeq[CardStatus] = findValues
}

sealed trait PaymentSystem extends EnumEntry

object PaymentSystem extends Enum[PaymentSystem] {

  case object Mir  extends PaymentSystem
  case object Visa extends PaymentSystem
  case object MC   extends PaymentSystem

  override def values: immutable.IndexedSeq[PaymentSystem] = findValues
}

final case class MoneyAmount(
  amount: BigDecimal,
  currency: Currency
)

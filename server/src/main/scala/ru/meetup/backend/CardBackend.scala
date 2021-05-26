package ru.meetup.backend

import cats.effect.Sync
import ru.meetup.backend.CardBackend.CardRaw

trait CardBackend[F[_]] {
  def getCards(bankAccountIds: List[String]): F[Map[String, List[CardRaw]]]
}

class CardBackendImpl[F[_]: Sync] extends CardBackend[F] {

  override def getCards(bankAccountIds: List[String]): F[Map[String, List[CardRaw]]] =
    Sync[F].delay(Map.empty)

}

object CardBackend {

  final case class CardRaw(
    id: String,
    cardNumber: String,
    paymentSystem: String
  )

}

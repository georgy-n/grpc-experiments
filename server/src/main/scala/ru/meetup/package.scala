package ru

import zio.{ZEnv, ZIO}

package object meetup {
  type MeetupTask[A] = ZIO[ZEnv, Throwable, A]
}

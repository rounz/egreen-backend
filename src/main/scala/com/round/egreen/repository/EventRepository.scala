// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.repository

import cats.data.EitherT
import cats.effect.{Effect, IO}
import cats.implicits._
import com.round.egreen.cqrs.event.EventEnvelope
import fs2._
import fs2.async.mutable.Queue
import org.mongodb.scala._
import org.mongodb.scala.bson.BsonBinary

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._

trait EventRepository[F[_]] {
  def eventStream(implicit F: Effect[F]): Stream[F, EventEnvelope]
  def saveEvent(event: EventEnvelope)(implicit F: Effect[F]): EitherT[F, String, Unit]
}

object EventRepository {
  val QUEUE_SIZE = 1000
}

final class MongoEventRepository[F[_]](mongo: MongoCollection[Document]) extends EventRepository[F] {
  import EventRepository._

  def eventStream(implicit F: Effect[F]): Stream[F, EventEnvelope] =
    for {
      queue    <- Stream.eval(async.boundedQueue[F, Either[Throwable, EventEnvelope]](QUEUE_SIZE))
      _        <- Stream.eval(F.delay(mongo.find().subscribe(new EventObserver(queue))))
      envelope <- queue.dequeue.takeWhile(_.isRight).rethrow
    } yield envelope

  def saveEvent(event: EventEnvelope)(implicit F: Effect[F]): EitherT[F, String, Unit] =
    EitherT(
      F.delay(mongo.insertOne(Document("payload" -> event.toByteArray)).toFuture).flatMap { f =>
        F.async { cb =>
          f.onComplete(
            r => cb(Either.fromTry(r.map(_ => ().asRight)))
          )(global)
        }
      }
    )

  private class EventObserver(queue: Queue[F, Either[Throwable, EventEnvelope]])(implicit F: Effect[F])
      extends Observer[Document] {

    private var seen: Long                         = 0
    private var subscription: Option[Subscription] = None

    override def onSubscribe(subscription: Subscription): Unit = {
      this.subscription = Some(subscription)
      subscription.request(QUEUE_SIZE)
    }

    override def onNext(doc: Document): Unit = {
      async.unsafeRunAsync(
        queue.enqueue1(Right(EventEnvelope.parseFrom(doc[BsonBinary]("payload").getData)))
      )(_ => IO.unit)
      seen += 1
      if (seen == QUEUE_SIZE) {
        seen = 0
        subscription.foreach(_.request(QUEUE_SIZE))
      }
    }

    override def onError(e: Throwable): Unit =
      async.unsafeRunAsync(
        queue.enqueue1(Left(e))
      )(_ => IO.unit)

    override def onComplete(): Unit =
      async.unsafeRunAsync(
        queue.enqueue1(Left(EventEnvelope.LAST_EVENT))
      )(_ => IO.unit)
  }
}

import { Injectable, NgZone, inject } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { GenerationEvent, GenerationStreamMessage } from '../models/generation-event.model';

interface BookStream {
  source: EventSource;
  subject: Subject<GenerationStreamMessage>;
  refCount: number;
}

/**
 * One SSE stream per book, shared and ref-counted across components. Replaces status polling:
 * the backend pushes a {@link GenerationEvent} whenever an async job changes state. `EventSource`
 * auto-reconnects on drop; a `connected` message is emitted on open and each reconnect so
 * subscribers can reconcile any state they might have missed.
 */
@Injectable({ providedIn: 'root' })
export class GenerationEventsService {
  private readonly zone = inject(NgZone);
  private readonly streams = new Map<number, BookStream>();

  /** Subscribe to a book's event stream. The underlying EventSource is opened lazily and
   *  closed when the last subscriber unsubscribes. */
  stream(bookId: number): Observable<GenerationStreamMessage> {
    return new Observable<GenerationStreamMessage>(observer => {
      const entry = this.acquire(bookId);
      const sub = entry.subject.subscribe(observer);
      return () => {
        sub.unsubscribe();
        this.release(bookId);
      };
    });
  }

  private acquire(bookId: number): BookStream {
    const existing = this.streams.get(bookId);
    if (existing) {
      existing.refCount++;
      return existing;
    }

    const subject = new Subject<GenerationStreamMessage>();
    const source = new EventSource(`/api/books/${bookId}/events`);

    source.onopen = () => this.zone.run(() => subject.next({ type: 'connected' }));
    source.onmessage = event => {
      try {
        const parsed = JSON.parse(event.data) as GenerationEvent;
        this.zone.run(() => subject.next({ type: 'event', event: parsed }));
      } catch {
        // Ignore non-JSON payloads (e.g. keep-alive comments).
      }
    };
    // On error EventSource reconnects on its own; onopen re-fires and triggers reconciliation.

    const created: BookStream = { source, subject, refCount: 1 };
    this.streams.set(bookId, created);
    return created;
  }

  private release(bookId: number): void {
    const entry = this.streams.get(bookId);
    if (!entry) {
      return;
    }
    entry.refCount--;
    if (entry.refCount <= 0) {
      entry.source.close();
      entry.subject.complete();
      this.streams.delete(bookId);
    }
  }
}

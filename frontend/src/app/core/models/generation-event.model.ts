import { ProcessingStatus } from './chapter.model';

export type GenerationEventKind =
  | 'CHAPTER'
  | 'EXTRACTION'
  | 'CHARACTERISTICS'
  | 'QUESTIONS'
  | 'REGENERATION';

/** A server-pushed status change for an async job of a book. */
export interface GenerationEvent {
  kind: GenerationEventKind;
  ordinal: number | null;
  questionId: number | null;
  status: ProcessingStatus;
  error: string | null;
}

/**
 * What the SSE stream emits: either a server event, or a client-side `connected` marker
 * (fired on open and on every auto-reconnect) so subscribers can reconcile their state.
 */
export type GenerationStreamMessage =
  | { type: 'event'; event: GenerationEvent }
  | { type: 'connected' };

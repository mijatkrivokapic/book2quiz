export type GenerationKind = 'CHARACTERISTICS' | 'QUESTIONS' | 'QUESTION_REGENERATION';

/** One entry in a chapter's generation/token-usage history. */
export interface GenerationRecord {
  id: number;
  type: GenerationKind;
  model: string;
  promptVersion: string | null;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
  durationMs: number;
  summary: string | null;
  createdAt: string;
}

export interface TypeUsage {
  count: number;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
}

/** Aggregated token usage for a chapter. */
export interface GenerationUsageSummary {
  recordCount: number;
  totalInputTokens: number;
  totalOutputTokens: number;
  totalTokens: number;
  byType: Partial<Record<GenerationKind, TypeUsage>>;
}

import { ProcessingStatus } from './chapter.model';

export type QuestionType = 'MULTIPLE_CHOICE' | 'MULTIPLE_RESPONSE' | 'SHORT_ANSWER';
export type QuestionStatus = 'PENDING' | 'APPROVED';
export type QuestionOrigin = 'GENERATED' | 'MANUAL';

export interface MrqOption {
  text: string;
  isCorrect: boolean;
  feedback: string;
  hints: string[];
}

/** The polymorphic question payload. Fields present depend on questionType. */
export interface QuestionPayload {
  questionType: QuestionType;
  text: string;
  hints: string[];
  // MULTIPLE_CHOICE
  distractors?: string[];
  correctOption?: string;
  feedback?: string;
  // MULTIPLE_RESPONSE
  options?: MrqOption[];
  // SHORT_ANSWER
  acceptableAnswers?: string[];
}

export interface Question {
  id: number;
  status: QuestionStatus;
  origin: QuestionOrigin;
  question: QuestionPayload;
  activeVersionId: number | null;
  versionCount: number;
  regenerationStatus: ProcessingStatus | null;
  regenerationError: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface QuestionVersion {
  id: number;
  active: boolean;
  status: QuestionStatus;
  origin: QuestionOrigin;
  guideline: string | null;
  question: QuestionPayload;
  createdAt: string;
  updatedAt: string;
}

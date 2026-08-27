export type ProcessingStatus = 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED';

export interface Chapter {
  id: number;
  ordinal: number;
  title: string;
  startPage: number;
  endPage: number;
  status: ProcessingStatus;
  errorMessage: string | null;
  markdownAvailable: boolean;
  pdfAvailable: boolean;
  createdAt: string;
  updatedAt: string;
}

export type ChapterSource = 'PDF' | 'MARKDOWN';

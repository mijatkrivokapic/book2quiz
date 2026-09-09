export type CharacteristicStatus = 'PENDING' | 'APPROVED';
export type CharacteristicOrigin = 'GENERATED' | 'MANUAL';

export interface Characteristic {
  id: number;
  content: string;
  status: CharacteristicStatus;
  origin: CharacteristicOrigin;
}

/**
 * A chapter's learning objective. It owns a list of structural characteristics (fetched
 * separately, per objective). Surface characteristics remain attached to the chapter.
 */
export interface LearningObjective {
  id: number;
  description: string;
  status: CharacteristicStatus;
  origin: CharacteristicOrigin;
}

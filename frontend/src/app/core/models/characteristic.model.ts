export type CharacteristicType = 'structural' | 'surface';
export type CharacteristicStatus = 'PENDING' | 'APPROVED';
export type CharacteristicOrigin = 'GENERATED' | 'MANUAL';

export interface Characteristic {
  id: number;
  content: string;
  status: CharacteristicStatus;
  origin: CharacteristicOrigin;
}

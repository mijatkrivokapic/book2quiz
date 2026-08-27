export interface Book {
  id: number;
  title: string;
  courseId: number;
  downloadUrl: string;
}

export interface UpdateBookRequest {
  title: string;
}

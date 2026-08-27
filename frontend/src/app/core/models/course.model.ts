export interface Course {
  id: number;
  name: string;
  bookCount: number;
  lessonCount: number;
}

export interface CreateCourseRequest {
  name: string;
}

export interface UpdateCourseRequest {
  name: string;
}

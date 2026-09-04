package com.example.book2quiz.service;

import com.example.book2quiz.dto.book.CreateBookDTO;
import com.example.book2quiz.dto.book.GetBookDTO;
import com.example.book2quiz.dto.book.UpdateBookDTO;
import com.example.book2quiz.exception.ResourceNotFoundException;
import com.example.book2quiz.model.Book;
import com.example.book2quiz.model.Course;
import com.example.book2quiz.repository.BookRepository;
import com.example.book2quiz.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional
public class BookService {

    private static final String STORAGE_DIRECTORY = "books";//TODO make configurable

    private final BookRepository bookRepository;
    private final CourseRepository courseRepository;
    private final FileStorageService fileStorageService;

    public BookService(BookRepository bookRepository, CourseRepository courseRepository, FileStorageService fileStorageService) {
        this.bookRepository = bookRepository;
        this.courseRepository = courseRepository;
        this.fileStorageService = fileStorageService;
    }

    public GetBookDTO createBook(CreateBookDTO dto) {
        Course course = findCourseOrThrow(dto.courseId());
        String fileKey = fileStorageService.uploadFile(dto.file(), STORAGE_DIRECTORY);

        Book book = new Book();
        book.setTitle(dto.title());
        book.setFileKey(fileKey);
        book.setCourse(course);

        Book saved = bookRepository.save(book);
        return toGetBookDTO(saved);
    }

    @Transactional(readOnly = true)
    public GetBookDTO getBook(Integer id) {
        return toGetBookDTO(findBookOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<GetBookDTO> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(this::toGetBookDTO)
                .toList();
    }

    public GetBookDTO updateBook(Integer id, UpdateBookDTO dto) {
        Book book = findBookOrThrow(id);
        book.setTitle(dto.title());
        return toGetBookDTO(book);
    }

    public GetBookDTO updateBookFile(Integer id, MultipartFile file) {
        Book book = findBookOrThrow(id);
        String oldFileKey = book.getFileKey();

        String newFileKey = fileStorageService.uploadFile(file, STORAGE_DIRECTORY);
        book.setFileKey(newFileKey);
        fileStorageService.deleteFile(oldFileKey);

        return toGetBookDTO(book);
    }

    public void deleteBook(Integer id) {
        Book book = findBookOrThrow(id);
        fileStorageService.deleteFile(book.getFileKey());
        bookRepository.delete(book);
    }

    private Book findBookOrThrow(Integer id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }

    private Course findCourseOrThrow(Integer id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }

    public GetBookDTO toGetBookDTO(Book book) {
        String downloadUrl = fileStorageService.getPreSignedUrl(book.getFileKey());
        return new GetBookDTO(book.getId(), book.getTitle(), book.getCourse().getId(), downloadUrl);
    }
}

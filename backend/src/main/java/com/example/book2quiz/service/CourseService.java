package com.example.book2quiz.service;

import com.example.book2quiz.dto.book.GetBookDTO;
import com.example.book2quiz.dto.course.CreateCourseDTO;
import com.example.book2quiz.dto.course.GetCourseDTO;
import com.example.book2quiz.dto.course.UpdateCourseDTO;
import com.example.book2quiz.exception.ResourceNotFoundException;
import com.example.book2quiz.model.Book;
import com.example.book2quiz.model.Course;
import com.example.book2quiz.repository.CourseRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CourseService{

    private final CourseRepository courseRepository;
    private final ModelMapper modelMapper;

    public CourseService(CourseRepository courseRepository, ModelMapper modelMapper) {
        this.courseRepository = courseRepository;
        this.modelMapper = modelMapper;
    }

    public GetCourseDTO createCourse(CreateCourseDTO dto) {
        Course course = modelMapper.map(dto, Course.class);
        Course saved = courseRepository.save(course);
        return modelMapper.map(saved, GetCourseDTO.class);
    }

    @Transactional(readOnly = true)
    public GetCourseDTO getCourse(Integer id) {
        return modelMapper.map(findCourseOrThrow(id), GetCourseDTO.class);
    }

    @Transactional(readOnly = true)
    public List<GetCourseDTO> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(course -> modelMapper.map(course, GetCourseDTO.class))
                .toList();
    }

    public GetCourseDTO updateCourse(Integer id, UpdateCourseDTO dto) {
        Course course = findCourseOrThrow(id);
        modelMapper.map(dto, course);
        return modelMapper.map(course, GetCourseDTO.class);
    }

    public void deleteCourse(Integer id) {
        Course course = findCourseOrThrow(id);
        courseRepository.delete(course);
    }

    private Course findCourseOrThrow(Integer id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }

    public List<GetBookDTO> getBooksByCourse(Integer id) {
        Course course = findCourseOrThrow(id);
        return course.getBooks().stream()
                .map(book -> modelMapper.map(book, GetBookDTO.class))
                .toList();
    }
}
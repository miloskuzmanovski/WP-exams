package mk.ukim.finki.wp.kol2022.g2.service.impl;

import mk.ukim.finki.wp.kol2022.g2.model.Course;
import mk.ukim.finki.wp.kol2022.g2.model.Student;
import mk.ukim.finki.wp.kol2022.g2.model.StudentType;
import mk.ukim.finki.wp.kol2022.g2.model.exceptions.InvalidCourseIdException;
import mk.ukim.finki.wp.kol2022.g2.model.exceptions.InvalidStudentIdException;
import mk.ukim.finki.wp.kol2022.g2.repository.CourseRepository;
import mk.ukim.finki.wp.kol2022.g2.repository.StudentRepository;
import mk.ukim.finki.wp.kol2022.g2.service.StudentService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class StudentServiceImpl implements StudentService, UserDetailsService {
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentServiceImpl(StudentRepository studentRepository, CourseRepository courseRepository, PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public List<Student> listAll() {
        return studentRepository.findAll();
    }

    @Override
    public Student findById(Long id) {
        return studentRepository.findById(id).orElseThrow(InvalidStudentIdException::new);
    }

    @Override
    public Student create(String name, String email, String password, StudentType type, List<Long> courseId, LocalDate enrollmentDate) {
        List<Course> coursesList = courseRepository.findAllById(courseId);
        if (coursesList.isEmpty()) throw new InvalidCourseIdException();
        return studentRepository.save(new Student(name, email, passwordEncoder.encode(password), type, coursesList, enrollmentDate));
    }

    @Override
    public Student update(Long id, String name, String email, String password, StudentType type, List<Long> coursesId, LocalDate enrollmentDate) {
        List<Course> coursesList = courseRepository.findAllById(coursesId);
        if (coursesList.isEmpty()) throw new InvalidCourseIdException();
        Student old = studentRepository.findById(id).orElseThrow(InvalidStudentIdException::new);
        old.setCourses(coursesList);
        old.setName(name);
        old.setEmail(email);
        old.setPassword(passwordEncoder.encode(password));
        old.setType(type);
        old.setEnrollmentDate(enrollmentDate);
        return studentRepository.save(old);
    }

    @Override
    public Student delete(Long id) {
        Student old = studentRepository.findById(id).orElseThrow(InvalidStudentIdException::new);
        studentRepository.delete(old);
        return old;
    }

    @Override
    public List<Student> filter(Long courseId, Integer yearsOfStudying) {
        List<Student> results;
        if (courseId == null && yearsOfStudying == null) {
            results = studentRepository.findAll();
        } else if (courseId != null && yearsOfStudying != null) {
            results = studentRepository.findAllByCoursesContainingAndEnrollmentDateBefore(
                    courseRepository.findById(courseId).orElseThrow(InvalidStudentIdException::new),
                    LocalDate.now().minusYears(yearsOfStudying)
            );
        } else if (courseId != null) {
            results = studentRepository.findAllByCoursesContaining(courseRepository.findById(courseId).orElseThrow(InvalidStudentIdException::new));
        } else {
            results = studentRepository.findAllByEnrollmentDateBefore(LocalDate.now().minusYears(yearsOfStudying));
        }

        return results;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Student s = studentRepository.findByEmail(username);
        return new User(s.getEmail(), s.getPassword(), Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + s.getType().toString())));
    }
}

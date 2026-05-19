package com.hostel.management.repository;

import com.hostel.management.model.Room;
import com.hostel.management.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByRegistrationNo(String registrationNo);
    boolean existsByRegistrationNo(String registrationNo);
    List<Student> findByRoomAndActiveTrue(Room room);
    List<Student> findByRoom(Room room);
    List<Student> findByRoomIdAndActiveTrue(Long roomId);
    int countByRoomIdAndActiveTrue(Long roomId);
    List<Student> findByActiveTrue();
    List<Student> findByNameContainingIgnoreCaseAndActiveTrue(String name);
}

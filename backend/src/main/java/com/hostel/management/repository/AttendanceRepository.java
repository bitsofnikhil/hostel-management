package com.hostel.management.repository;

import com.hostel.management.model.Attendance;
import com.hostel.management.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByStudentAndAttendanceDate(Student student, LocalDate date);
    List<Attendance> findByAttendanceDate(LocalDate date);
    boolean existsByAttendanceDate(LocalDate date);
    long countByAttendanceDate(LocalDate date);

    @Query("SELECT DISTINCT a.attendanceDate FROM Attendance a ORDER BY a.attendanceDate DESC")
    List<LocalDate> findDistinctAttendanceDatesDesc();
    List<Attendance> findByStudentAndAttendanceDateBetween(Student student, LocalDate from, LocalDate to);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.attendanceDate = :date AND a.status = 'PRESENT' AND a.student.room.id = :roomId")
    long countPresentByDateAndRoom(@Param("date") LocalDate date, @Param("roomId") Long roomId);
}

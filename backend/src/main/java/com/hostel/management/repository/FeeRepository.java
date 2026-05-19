package com.hostel.management.repository;

import com.hostel.management.model.Fee;
import com.hostel.management.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeeRepository extends JpaRepository<Fee, Long> {
    List<Fee> findByStudent(Student student);
    List<Fee> findByStatus(Fee.FeeStatus status);
    List<Fee> findByStudentOrderByDueDateDesc(Student student);

    @Query("""
            SELECT f FROM Fee f
            WHERE f.student = :student
              AND LOWER(f.feeType) = LOWER(:feeType)
              AND f.billingPeriod = :billingPeriod
              AND f.dueDate BETWEEN :yearStart AND :yearEnd
            ORDER BY f.id DESC
            """)
    List<Fee> findPossibleDuplicates(
            @Param("student") Student student,
            @Param("feeType") String feeType,
            @Param("billingPeriod") Fee.BillingPeriod billingPeriod,
            @Param("yearStart") LocalDate yearStart,
            @Param("yearEnd") LocalDate yearEnd
    );
}


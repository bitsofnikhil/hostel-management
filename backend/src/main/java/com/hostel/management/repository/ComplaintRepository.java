package com.hostel.management.repository;

import com.hostel.management.model.Complaint;
import com.hostel.management.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByStudentOrderByRaisedAtDesc(Student student);
    List<Complaint> findByStatusOrderByRaisedAtDesc(Complaint.ComplaintStatus status);
    List<Complaint> findByCategoryOrderByRaisedAtDesc(Complaint.ComplaintCategory category);
    List<Complaint> findAllByOrderByRaisedAtDesc();
}

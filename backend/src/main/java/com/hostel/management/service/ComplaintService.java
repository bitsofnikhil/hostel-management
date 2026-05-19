package com.hostel.management.service;

import com.hostel.management.dto.ComplaintRequest;
import com.hostel.management.model.Complaint;
import com.hostel.management.model.Room;
import com.hostel.management.model.Student;
import com.hostel.management.repository.ComplaintRepository;
import com.hostel.management.repository.RoomRepository;
import com.hostel.management.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;

    public ComplaintService(ComplaintRepository complaintRepository, StudentRepository studentRepository, RoomRepository roomRepository) {
        this.complaintRepository = complaintRepository;
        this.studentRepository = studentRepository;
        this.roomRepository = roomRepository;
    }

    public List<Complaint> getAllComplaints() { return complaintRepository.findAllByOrderByRaisedAtDesc(); }
    public List<Complaint> getComplaintsByStatus(Complaint.ComplaintStatus status) { return complaintRepository.findByStatusOrderByRaisedAtDesc(status); }
    public List<Complaint> getComplaintsByCategory(Complaint.ComplaintCategory category) { return complaintRepository.findByCategoryOrderByRaisedAtDesc(category); }

    public List<Complaint> getComplaintsByStudent(Long studentId) {
        return complaintRepository.findByStudentOrderByRaisedAtDesc(findStudent(studentId));
    }

    @Transactional
    public Complaint createComplaint(ComplaintRequest request) {
        Complaint complaint = new Complaint();
        complaint.setTitle(request.getTitle());
        complaint.setDescription(request.getDescription());
        complaint.setRaisedAt(LocalDateTime.now());
        complaint.setComplaintDate(request.getComplaintDate() != null ? request.getComplaintDate() : LocalDate.now());
        complaint.setCategory(request.getCategory() != null ? request.getCategory() : Complaint.ComplaintCategory.HOSTEL);
        complaint.setStatus(Complaint.ComplaintStatus.OPEN);
        if (request.getStudentId() != null) {
            Student student = findStudent(request.getStudentId());
            complaint.setStudent(student);
            if (student.getRoom() != null) complaint.setRoom(student.getRoom());
        }
        if (request.getRoomId() != null) complaint.setRoom(findRoom(request.getRoomId()));
        return complaintRepository.save(complaint);
    }

    @Transactional
    public Complaint createAttendanceRoomComplaint(Student student, LocalDate date, String text) {
        if (text == null || text.isBlank()) return null;
        Complaint c = new Complaint();
        c.setStudent(student);
        c.setRoom(student.getRoom());
        c.setCategory(Complaint.ComplaintCategory.ROOM);
        c.setTitle("Room complaint during attendance");
        c.setDescription(text.trim());
        c.setComplaintDate(date);
        c.setRaisedAt(LocalDateTime.now());
        c.setStatus(Complaint.ComplaintStatus.OPEN);
        return complaintRepository.save(c);
    }


    @Transactional
    public Complaint createAttendanceRoomComplaint(Room room, LocalDate date, String text) {
        if (room == null || text == null || text.isBlank()) return null;
        Complaint c = new Complaint();
        c.setRoom(room);
        c.setCategory(Complaint.ComplaintCategory.ROOM);
        c.setTitle("Room complaint during attendance");
        c.setDescription(text.trim());
        c.setComplaintDate(date);
        c.setRaisedAt(LocalDateTime.now());
        c.setStatus(Complaint.ComplaintStatus.OPEN);
        return complaintRepository.save(c);
    }

    @Transactional
    public Complaint createCommonComplaint(LocalDate date, String title, String text, Complaint.ComplaintCategory category) {
        if (text == null || text.isBlank()) return null;
        Complaint c = new Complaint();
        c.setCategory(category != null ? category : Complaint.ComplaintCategory.HOSTEL);
        c.setTitle(title == null || title.isBlank() ? "Common hostel complaint" : title);
        c.setDescription(text.trim());
        c.setComplaintDate(date != null ? date : LocalDate.now());
        c.setRaisedAt(LocalDateTime.now());
        c.setStatus(Complaint.ComplaintStatus.OPEN);
        return complaintRepository.save(c);
    }

    @Transactional
    public Complaint updateComplaintStatus(Long id, Complaint.ComplaintStatus status, String resolution) {
        Complaint complaint = findComplaintById(id);
        complaint.setStatus(status);
        if (status == Complaint.ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(LocalDateTime.now());
            if (resolution != null) complaint.setResolution(resolution);
        }
        return complaintRepository.save(complaint);
    }

    @Transactional public void deleteComplaint(Long id) { complaintRepository.delete(findComplaintById(id)); }

    private Complaint findComplaintById(Long id) { return complaintRepository.findById(id).orElseThrow(() -> new RuntimeException("Complaint not found: " + id)); }
    private Student findStudent(Long id) { return studentRepository.findById(id).orElseThrow(() -> new RuntimeException("Student not found: " + id)); }
    private Room findRoom(Long id) { return roomRepository.findById(id).orElseThrow(() -> new RuntimeException("Room not found: " + id)); }
}

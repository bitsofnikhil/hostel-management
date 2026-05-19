package com.hostel.management.service;

import com.hostel.management.dto.StudentRequest;
import com.hostel.management.dto.StudentResponse;
import com.hostel.management.model.Room;
import com.hostel.management.model.Student;
import com.hostel.management.repository.RoomRepository;
import com.hostel.management.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final RoomService roomService;

    public StudentService(StudentRepository studentRepository, RoomRepository roomRepository, RoomService roomService) {
        this.studentRepository = studentRepository;
        this.roomRepository = roomRepository;
        this.roomService = roomService;
    }

    public List<StudentResponse> getAllStudents() {
        roomService.normalizeSplitRoomRecords();
        return sortStudents(studentRepository.findByActiveTrue()).stream().map(StudentResponse::fromStudent).collect(Collectors.toList());
    }

    public StudentResponse getStudentById(Long id) {
        return StudentResponse.fromStudent(findStudentById(id));
    }

    public StudentResponse getStudentByRegNo(String regNo) {
        Student student = studentRepository.findByRegistrationNo(regNo)
                .orElseThrow(() -> new RuntimeException("Student not found with registration no: " + regNo));
        return StudentResponse.fromStudent(student);
    }

    public List<StudentResponse> searchStudents(String name) {
        return sortStudents(studentRepository.findByNameContainingIgnoreCaseAndActiveTrue(name)).stream()
                .map(StudentResponse::fromStudent).collect(Collectors.toList());
    }

    @Transactional
    public StudentResponse createStudent(StudentRequest request) {
        if (studentRepository.existsByRegistrationNo(request.getRegistrationNo())) {
            throw new RuntimeException("Registration number already exists: " + request.getRegistrationNo());
        }
        Student student = new Student();
        applyRequest(student, request, true);
        student = studentRepository.save(student);
        if (student.getRoom() != null) roomService.updateOccupancy(student.getRoom().getId());
        return StudentResponse.fromStudent(student);
    }

    @Transactional
    public StudentResponse upsertStudent(StudentRequest request) {
        Student student = studentRepository.findByRegistrationNo(request.getRegistrationNo()).orElse(null);
        if (student == null) return createStudent(request);
        return updateStudent(student.getId(), request);
    }

    @Transactional
    public StudentResponse updateStudent(Long id, StudentRequest request) {
        Student student = findStudentById(id);
        Long oldRoomId = student.getRoom() != null ? student.getRoom().getId() : null;
        if (!student.getRegistrationNo().equals(request.getRegistrationNo()) && studentRepository.existsByRegistrationNo(request.getRegistrationNo())) {
            throw new RuntimeException("Registration number already exists: " + request.getRegistrationNo());
        }
        applyRequest(student, request, false);
        student = studentRepository.save(student);
        if (oldRoomId != null) roomService.updateOccupancy(oldRoomId);
        if (student.getRoom() != null) roomService.updateOccupancy(student.getRoom().getId());
        return StudentResponse.fromStudent(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = findStudentById(id);
        Long roomId = student.getRoom() != null ? student.getRoom().getId() : null;
        student.setActive(false);
        student.setRoom(null);
        studentRepository.save(student);
        if (roomId != null) roomService.updateOccupancy(roomId);
    }

    @Transactional
    public int deleteAllStudents() {
        List<Student> activeStudents = studentRepository.findByActiveTrue();
        int removed = activeStudents.size();
        activeStudents.forEach(student -> {
            student.setActive(false);
            student.setRoom(null);
        });
        studentRepository.saveAll(activeStudents);
        roomRepository.findAll().forEach(room -> {
            room.setCurrentOccupancy(0);
            roomRepository.save(room);
        });
        return removed;
    }

    private void applyRequest(Student student, StudentRequest request, boolean createMode) {
        student.setRegistrationNo(clean(request.getRegistrationNo()));
        student.setName(clean(request.getName()));
        student.setFatherName(defaultText(request.getFatherName(), "N/A"));
        student.setPhone(defaultText(request.getPhone(), "0000000000"));
        student.setEmail(defaultText(request.getEmail(), student.getRegistrationNo().toLowerCase() + "@hostel.local"));
        student.setAddress(defaultText(request.getAddress(), "N/A"));
        student.setCategory(defaultText(request.getCategory(), "GENERAL").toUpperCase());
        if (request.getImportOrder() != null) student.setImportOrder(request.getImportOrder());
        student.setActive(true);

        if (request.getRoomId() != null) {
            assignRoom(student, request.getRoomId());
        } else if (createMode && Boolean.TRUE.equals(request.getAutoAllocateRoom())) {
            Room room = findAutoAllocationRoom(request.getPreferredBlock());
            if (room != null) student.setRoom(room);
        }
    }

    private Room findAutoAllocationRoom(String preferredBlock) {
        roomService.normalizeSplitRoomRecords();
        if (preferredBlock != null && !preferredBlock.isBlank()) {
            Room blockRoom = roomRepository.findAvailableRoomsByBlock(preferredBlock.trim()).stream().findFirst().orElse(null);
            if (blockRoom != null) return blockRoom;
        }
        return roomRepository.findAvailableRooms().stream().findFirst().orElse(null);
    }

    private void assignRoom(Student student, Long roomId) {
        roomService.normalizeSplitRoomRecords();
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new RuntimeException("Room not found: " + roomId));
        if (student.getRoom() == null || !student.getRoom().getId().equals(room.getId())) {
            long activeStudents = studentRepository.countByRoomIdAndActiveTrue(room.getId());
            if (activeStudents >= room.getCapacity()) throw new RuntimeException("Room " + room.getRoomNumber() + " is at full capacity");
        }
        student.setRoom(room);
    }

    public Student findStudentById(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
    }

    public Student findStudentByRegistrationNo(String registrationNo) {
        return studentRepository.findByRegistrationNo(registrationNo).orElseThrow(() -> new RuntimeException("Student not found: " + registrationNo));
    }

    public List<Student> findActiveStudents() {
        roomService.normalizeSplitRoomRecords();
        return new ArrayList<>(sortStudents(studentRepository.findByActiveTrue()));
    }


    public List<Student> sortStudents(List<Student> students) {
        return students.stream()
                .sorted(Comparator
                        .comparing((Student s) -> s.getImportOrder() == null ? Integer.MAX_VALUE : s.getImportOrder())
                        .thenComparing(s -> firstNumber(s.getRegistrationNo()))
                        .thenComparing(s -> safeText(s.getRegistrationNo()))
                        .thenComparing(s -> safeText(s.getName())))
                .collect(Collectors.toList());
    }

    private Integer firstNumber(String value) {
        if (value == null) return Integer.MAX_VALUE;
        String digits = value.replaceAll("^\\D*(\\d+).*$", "$1");
        if (digits.equals(value) && !value.matches(".*\\d.*")) return Integer.MAX_VALUE;
        try { return Integer.parseInt(digits); } catch (Exception e) { return Integer.MAX_VALUE; }
    }

    private String safeText(String value) { return value == null ? "" : value.trim().toLowerCase(); }

    private String clean(String v) { return v == null ? null : v.trim(); }
    private String defaultText(String v, String fallback) { return v == null || v.isBlank() ? fallback : v.trim(); }
}

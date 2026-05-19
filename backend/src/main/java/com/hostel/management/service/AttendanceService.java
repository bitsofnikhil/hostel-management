package com.hostel.management.service;

import com.hostel.management.dto.AttendanceRequest;
import com.hostel.management.model.Attendance;
import com.hostel.management.model.Room;
import com.hostel.management.model.Student;
import com.hostel.management.repository.AttendanceRepository;
import com.hostel.management.repository.RoomRepository;
import com.hostel.management.repository.StudentRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final ComplaintService complaintService;
    private final StudentService studentService;

    public AttendanceService(AttendanceRepository attendanceRepository, StudentRepository studentRepository, RoomRepository roomRepository, ComplaintService complaintService, StudentService studentService) {
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.roomRepository = roomRepository;
        this.complaintService = complaintService;
        this.studentService = studentService;
    }

    @Transactional
    public List<Map<String, Object>> saveAttendance(AttendanceRequest request) {
        LocalDate date = request.getDate() != null ? request.getDate() : LocalDate.now();
        if (request.getCommonComplaint() != null && !request.getCommonComplaint().isBlank()) {
            complaintService.createCommonComplaint(date, "Common hostel complaint during attendance", request.getCommonComplaint(), null);
        }
        if (request.getRoomComplaints() != null) {
            request.getRoomComplaints().forEach((roomId, text) -> {
                if (text != null && !text.isBlank()) {
                    Room room = roomRepository.findById(roomId).orElseThrow(() -> new RuntimeException("Room not found: " + roomId));
                    complaintService.createAttendanceRoomComplaint(room, date, text);
                }
            });
        }

        List<Map<String, Object>> result = new ArrayList<>();
        if (request.getRecords() == null) return result;
        for (AttendanceRequest.StudentAttendance record : request.getRecords()) {
            Student student = studentRepository.findById(record.getStudentId()).orElseThrow(() -> new RuntimeException("Student not found: " + record.getStudentId()));
            Attendance attendance = attendanceRepository.findByStudentAndAttendanceDate(student, date).orElse(new Attendance());
            attendance.setStudent(student);
            attendance.setAttendanceDate(date);
            attendance.setStatus(record.getStatus() != null ? record.getStatus() : Attendance.AttendanceStatus.ABSENT);
            attendance.setRemarks(record.getRemarks());
            attendance.setRoomComplaint(null);
            attendance.setMarkedAt(LocalDateTime.now());
            attendanceRepository.save(attendance);
            Map<String, Object> entry = new HashMap<>();
            entry.put("studentId", student.getId());
            entry.put("studentName", student.getName());
            entry.put("status", attendance.getStatus());
            result.add(entry);
        }
        return result;
    }

    public List<Map<String, Object>> getAttendanceByRoom(Long roomId, LocalDate date) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new RuntimeException("Room not found: " + roomId));
        List<Student> students = studentService.sortStudents(studentRepository.findByRoomAndActiveTrue(room));
        if (date == null) date = LocalDate.now();
        final LocalDate finalDate = date;
        List<Map<String, Object>> result = new ArrayList<>();
        for (Student student : students) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("studentId", student.getId());
            entry.put("studentName", student.getName());
            entry.put("registrationNo", student.getRegistrationNo());
            Attendance att = attendanceRepository.findByStudentAndAttendanceDate(student, finalDate).orElse(null);
            entry.put("status", att != null ? att.getStatus().name() : "ABSENT");
            entry.put("remarks", att != null ? att.getRemarks() : null);
            entry.put("roomComplaint", att != null ? att.getRoomComplaint() : null);
            entry.put("markedAt", att != null ? att.getMarkedAt() : null);
            result.add(entry);
        }
        long presentCount = result.stream().filter(e -> "PRESENT".equals(e.get("status"))).count();
        Map<String, Object> summary = new HashMap<>();
        summary.put("roomId", roomId);
        summary.put("roomNumber", room.getRoomNumber());
        summary.put("block", room.getBlock());
        summary.put("floor", room.getFloor());
        summary.put("totalStudents", students.size());
        summary.put("presentCount", presentCount);
        summary.put("date", finalDate.toString());
        summary.put("students", result);
        return List.of(summary);
    }

    public List<Map<String, Object>> getAllRoomsAttendanceSummary(LocalDate date) {
        if (date == null) date = LocalDate.now();
        List<Room> rooms = roomRepository.findAllByOrderByBlockAscFloorAscRoomNumberAsc();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Room room : rooms) {
            List<Student> students = studentService.sortStudents(studentRepository.findByRoomAndActiveTrue(room));
            if (students.isEmpty()) continue;
            long presentCount = attendanceRepository.countPresentByDateAndRoom(date, room.getId());
            Map<String, Object> entry = new HashMap<>();
            entry.put("roomId", room.getId());
            entry.put("roomNumber", room.getRoomNumber());
            entry.put("block", room.getBlock());
            entry.put("floor", room.getFloor());
            entry.put("totalStudents", students.size());
            entry.put("capacity", room.getCapacity());
            entry.put("presentCount", presentCount);
            entry.put("date", date.toString());
            result.add(entry);
        }
        return result;
    }

    public List<Map<String, Object>> getNightAttendanceRound(LocalDate date, String block) {
        if (date == null) date = LocalDate.now();
        List<Room> rooms = (block == null || block.isBlank())
                ? roomRepository.findAllByOrderByBlockAscFloorAscRoomNumberAsc()
                : roomRepository.findByBlockOrderByFloorAscRoomNumberAsc(block.trim().toUpperCase());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Room room : rooms) {
            List<Student> students = studentService.sortStudents(studentRepository.findByRoomAndActiveTrue(room));
            Map<String, Object> summary = getAttendanceByRoom(room.getId(), date).get(0);
            summary.put("capacity", room.getCapacity());
            summary.put("freeBeds", Math.max(0, room.getCapacity() - students.size()));
            summary.put("block", room.getBlock());
            result.add(summary);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> saveIndividualAttendance(Long studentId, LocalDate date, Attendance.AttendanceStatus status, String remarks, String roomComplaint) {
        if (date == null) date = LocalDate.now();
        Student student = studentRepository.findById(studentId).orElseThrow(() -> new RuntimeException("Student not found: " + studentId));
        Attendance attendance = attendanceRepository.findByStudentAndAttendanceDate(student, date).orElse(new Attendance());
        attendance.setStudent(student);
        attendance.setAttendanceDate(date);
        attendance.setStatus(status != null ? status : Attendance.AttendanceStatus.ABSENT);
        attendance.setRemarks(remarks);
        attendance.setRoomComplaint(roomComplaint);
        attendance.setMarkedAt(LocalDateTime.now());
        attendanceRepository.save(attendance);
        if (roomComplaint != null && !roomComplaint.isBlank()) {
            complaintService.createAttendanceRoomComplaint(student, date, roomComplaint);
        }
        Map<String, Object> entry = new HashMap<>();
        entry.put("studentId", student.getId());
        entry.put("studentName", student.getName());
        entry.put("registrationNo", student.getRegistrationNo());
        entry.put("date", date.toString());
        entry.put("status", attendance.getStatus().name());
        entry.put("remarks", attendance.getRemarks());
        entry.put("roomComplaint", attendance.getRoomComplaint());
        entry.put("markedAt", attendance.getMarkedAt());
        return entry;
    }

    public List<Map<String, Object>> getStudentAttendance(Long studentId, LocalDate from, LocalDate to) {
        Student student = studentRepository.findById(studentId).orElseThrow(() -> new RuntimeException("Student not found: " + studentId));
        if (from == null) from = LocalDate.now().minusMonths(1);
        if (to == null) to = LocalDate.now();
        return attendanceRepository.findByStudentAndAttendanceDateBetween(student, from, to).stream().map(a -> {
            Map<String, Object> entry = new HashMap<>();
            entry.put("date", a.getAttendanceDate().toString());
            entry.put("status", a.getStatus().name());
            entry.put("remarks", a.getRemarks());
            entry.put("roomComplaint", a.getRoomComplaint());
            entry.put("markedAt", a.getMarkedAt());
            return entry;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> getAttendanceDateStatus(LocalDate date) {
        if (date == null) date = LocalDate.now();
        List<Attendance> rows = attendanceRepository.findByAttendanceDate(date);
        long present = rows.stream().filter(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT).count();
        long absent = rows.stream().filter(a -> a.getStatus() == Attendance.AttendanceStatus.ABSENT).count();
        long leave = rows.stream().filter(a -> a.getStatus() == Attendance.AttendanceStatus.LEAVE).count();
        int activeStudents = studentRepository.findByActiveTrue().size();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("date", date.toString());
        out.put("marked", !rows.isEmpty());
        out.put("totalMarked", rows.size());
        out.put("totalStudents", activeStudents);
        out.put("presentCount", present);
        out.put("absentCount", absent);
        out.put("leaveCount", leave);
        out.put("completed", activeStudents > 0 && rows.size() >= activeStudents);
        out.put("markedAt", rows.stream().map(Attendance::getMarkedAt).filter(Objects::nonNull).max(LocalDateTime::compareTo).map(Object::toString).orElse(null));
        return out;
    }

    public List<Map<String, Object>> getAttendanceDates(int limit) {
        int max = limit <= 0 ? 30 : Math.min(limit, 100);
        return attendanceRepository.findDistinctAttendanceDatesDesc().stream()
                .limit(max)
                .map(this::getAttendanceDateStatus)
                .collect(Collectors.toList());
    }

    public byte[] exportAttendanceExcel(LocalDate date, String block) {
        if (date == null) date = LocalDate.now();
        List<Map<String, Object>> rooms = getNightAttendanceRound(date, block);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Attendance");
            String[] headers = {"Date", "Block", "Room", "Registration No", "Student Name", "Status", "Remarks", "Room Complaint", "Marked At"};
            Row header = sheet.createRow(0);
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(style);
            }
            int r = 1;
            for (Map<String, Object> room : rooms) {
                List<Map<String, Object>> students = (List<Map<String, Object>>) room.getOrDefault("students", List.of());
                for (Map<String, Object> s : students) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(date.toString());
                    row.createCell(1).setCellValue(String.valueOf(room.getOrDefault("block", "")));
                    row.createCell(2).setCellValue(String.valueOf(room.getOrDefault("roomNumber", "")));
                    row.createCell(3).setCellValue(String.valueOf(s.getOrDefault("registrationNo", "")));
                    row.createCell(4).setCellValue(String.valueOf(s.getOrDefault("studentName", "")));
                    row.createCell(5).setCellValue(String.valueOf(s.getOrDefault("status", "")));
                    row.createCell(6).setCellValue(String.valueOf(s.getOrDefault("remarks", "")));
                    row.createCell(7).setCellValue(String.valueOf(s.getOrDefault("roomComplaint", "")));
                    row.createCell(8).setCellValue(String.valueOf(s.getOrDefault("markedAt", "")));
                }
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Could not export attendance Excel", e);
        }
    }

    public byte[] exportAttendancePdf(LocalDate date, String block) {
        if (date == null) date = LocalDate.now();
        List<Map<String, Object>> rooms = getNightAttendanceRound(date, block);
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream content = new PDPageContentStream(document, page);
            float y = 800;
            content.setFont(PDType1Font.HELVETICA_BOLD, 14);
            content.beginText();
            content.newLineAtOffset(40, y);
            content.showText("Hostel Night Attendance - " + date + (block == null || block.isBlank() ? "" : " - Block " + block));
            content.endText();
            y -= 28;
            content.setFont(PDType1Font.HELVETICA, 9);
            for (Map<String, Object> room : rooms) {
                List<Map<String, Object>> students = (List<Map<String, Object>>) room.getOrDefault("students", List.of());
                if (y < 80) {
                    content.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    content.setFont(PDType1Font.HELVETICA, 9);
                    y = 800;
                }
                content.beginText();
                content.newLineAtOffset(40, y);
                content.showText(cleanPdfText("Block " + room.getOrDefault("block", "") + " Room " + room.getOrDefault("roomNumber", "") + " | Capacity " + room.getOrDefault("capacity", "") + " | Present " + room.getOrDefault("presentCount", "0") + "/" + room.getOrDefault("totalStudents", "0")));
                content.endText();
                y -= 16;
                for (Map<String, Object> s : students) {
                    if (y < 60) {
                        content.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        content = new PDPageContentStream(document, page);
                        content.setFont(PDType1Font.HELVETICA, 9);
                        y = 800;
                    }
                    String line = s.getOrDefault("registrationNo", "") + " | " + s.getOrDefault("studentName", "") + " | " + s.getOrDefault("status", "") + " | " + Objects.toString(s.get("roomComplaint"), "");
                    content.beginText();
                    content.newLineAtOffset(55, y);
                    content.showText(cleanPdfText(line.length() > 110 ? line.substring(0, 110) : line));
                    content.endText();
                    y -= 13;
                }
                y -= 8;
            }
            content.close();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Could not export attendance PDF", e);
        }
    }

    private String cleanPdfText(String value) {
        return value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", " ");
    }

}

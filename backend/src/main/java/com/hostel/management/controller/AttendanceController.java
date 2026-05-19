package com.hostel.management.controller;

import com.hostel.management.dto.AttendanceRequest;
import com.hostel.management.model.Attendance;
import com.hostel.management.service.AttendanceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping("/summary")
    public ResponseEntity<List<Map<String, Object>>> getAllRoomsSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.getAllRoomsAttendanceSummary(date));
    }

    @GetMapping("/night-round")
    public ResponseEntity<List<Map<String, Object>>> getNightRound(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String block) {
        return ResponseEntity.ok(attendanceService.getNightAttendanceRound(date, block));
    }


    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDateStatus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.getAttendanceDateStatus(date));
    }

    @GetMapping("/dates")
    public ResponseEntity<List<Map<String, Object>>> getAttendanceDates(
            @RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(attendanceService.getAttendanceDates(limit));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<Map<String, Object>>> getRoomAttendance(
            @PathVariable Long roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.getAttendanceByRoom(roomId, date));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Map<String, Object>>> getStudentAttendance(
            @PathVariable Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(attendanceService.getStudentAttendance(studentId, from, to));
    }

    @PostMapping
    public ResponseEntity<List<Map<String, Object>>> saveAttendance(@RequestBody AttendanceRequest request) {
        return ResponseEntity.ok(attendanceService.saveAttendance(request));
    }

    @PostMapping("/student/{studentId}")
    public ResponseEntity<Map<String, Object>> saveIndividualAttendance(
            @PathVariable Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Attendance.AttendanceStatus status,
            @RequestParam(required = false) String remarks,
            @RequestParam(required = false) String roomComplaint) {
        return ResponseEntity.ok(attendanceService.saveIndividualAttendance(studentId, date, status, remarks, roomComplaint));
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String block) {
        LocalDate exportDate = date != null ? date : LocalDate.now();
        byte[] data = attendanceService.exportAttendanceExcel(exportDate, block);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance-" + exportDate + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String block) {
        LocalDate exportDate = date != null ? date : LocalDate.now();
        byte[] data = attendanceService.exportAttendancePdf(exportDate, block);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance-" + exportDate + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }
}

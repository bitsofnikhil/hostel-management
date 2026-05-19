package com.hostel.management.dto;

import com.hostel.management.model.Attendance;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class AttendanceRequest {
    private LocalDate date;
    private List<StudentAttendance> records;
    private String commonComplaint;
    private Map<Long, String> roomComplaints;

    @Data
    public static class StudentAttendance {
        private Long studentId;
        private Attendance.AttendanceStatus status;
        private String remarks;
        private String roomComplaint;
    }
}

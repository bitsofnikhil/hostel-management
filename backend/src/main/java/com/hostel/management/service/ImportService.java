package com.hostel.management.service;

import com.hostel.management.dto.AttendanceRequest;
import com.hostel.management.dto.StudentRequest;
import com.hostel.management.model.Attendance;
import com.hostel.management.model.Room;
import com.hostel.management.model.Student;
import com.hostel.management.repository.RoomRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ImportService {
    private final StudentService studentService;
    private final AttendanceService attendanceService;
    private final RoomRepository roomRepository;

    public ImportService(StudentService studentService, AttendanceService attendanceService, RoomRepository roomRepository) {
        this.studentService = studentService;
        this.attendanceService = attendanceService;
        this.roomRepository = roomRepository;
    }

    public Map<String, Object> importStudents(MultipartFile file) throws Exception {
        List<List<String>> rows = readRows(file);
        int headerRow = findStudentHeaderRow(rows);
        Map<String, Integer> columns = headerRow >= 0 ? buildColumnMap(rows.get(headerRow)) : Map.of();
        int startRow = headerRow >= 0 ? headerRow + 1 : 0;
        int createdOrUpdated = 0;
        List<String> errors = new ArrayList<>();
        normalizeExistingSplitRoomRecords();

        for (int i = startRow; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            if (r.isEmpty() || isBlankRow(r) || looksLikeHeader(r)) continue;
            try {
                StudentImportRow parsed = parseStudentRow(r, columns);
                if (parsed.registrationNo.isBlank() || parsed.name.isBlank()) continue;

                StudentRequest req = new StudentRequest();
                req.setRegistrationNo(parsed.registrationNo);
                req.setName(parsed.name);
                req.setFatherName(defaultText(parsed.fatherName, "N/A"));
                req.setPhone(normalizePhone(parsed.phone));
                req.setEmail(defaultText(parsed.email, parsed.registrationNo.toLowerCase(Locale.ROOT) + "@hostel.local"));
                req.setAddress(defaultText(parsed.address, "N/A"));
                req.setCategory(defaultText(parsed.category, "GENERAL").toUpperCase(Locale.ROOT));
                req.setPreferredBlock(defaultText(parsed.block, "A").toUpperCase(Locale.ROOT));
                req.setImportOrder(i + 1);

                if (!parsed.roomNumber.isBlank()) {
                    Room room = findOrCreateRoom(req.getPreferredBlock(), parsed.roomNumber);
                    req.setRoomId(room.getId());
                    req.setAutoAllocateRoom(false);
                }

                studentService.upsertStudent(req);
                createdOrUpdated++;
            } catch (Exception ex) {
                errors.add("Row " + (i + 1) + ": " + ex.getMessage());
            }
        }
        return result(createdOrUpdated, errors, "students");
    }

    public Map<String, Object> importAttendance(MultipartFile file) throws Exception {
        List<List<String>> rows = readRows(file);
        int saved = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            if (r.isEmpty() || looksLikeHeader(r)) continue;
            try {
                LocalDate date = parseDate(cell(r, 0));
                String reg = cell(r, 1);
                String statusText = defaultText(cell(r, 2), "ABSENT");
                String roomComplaint = cell(r, 3);
                String commonComplaint = cell(r, 4);
                Student student = studentService.findStudentByRegistrationNo(reg);
                AttendanceRequest request = new AttendanceRequest();
                request.setDate(date);
                request.setCommonComplaint(commonComplaint);
                AttendanceRequest.StudentAttendance rec = new AttendanceRequest.StudentAttendance();
                rec.setStudentId(student.getId());
                rec.setStatus(parseStatus(statusText));
                rec.setRoomComplaint(roomComplaint);
                request.setRecords(List.of(rec));
                attendanceService.saveAttendance(request);
                saved++;
            } catch (Exception ex) {
                errors.add("Row " + (i + 1) + ": " + ex.getMessage());
            }
        }
        return result(saved, errors, "attendance records");
    }

    private StudentImportRow parseStudentRow(List<String> r, Map<String, Integer> columns) {
        if (!columns.isEmpty()) {
            String roomRaw = first(r, columns, "roomnumber", "roomno", "room");
            String block = first(r, columns, "block");
            if (block.isBlank()) block = extractBlock(roomRaw);
            return new StudentImportRow(
                    first(r, columns, "registrationno", "registration", "regno", "serial", "sno", "srno"),
                    first(r, columns, "name", "studentname", "nameofstudents"),
                    first(r, columns, "fathername", "fathersname", "father"),
                    first(r, columns, "phone", "contact", "mobile"),
                    first(r, columns, "email", "emailaddress"),
                    first(r, columns, "address"),
                    defaultText(block, "A").toUpperCase(Locale.ROOT),
                    cleanRoomNumber(roomRaw),
                    first(r, columns, "category", "caste")
            );
        }

        String col6 = cell(r, 6);
        String col7 = cell(r, 7);
        String block;
        String roomNo;
        if (looksLikeBlock(col6) || (!col7.isBlank() && !looksLikeBlock(col7))) {
            block = defaultText(col6, "A").toUpperCase(Locale.ROOT);
            roomNo = col7;
        } else {
            roomNo = col6;
            block = defaultText(col7, extractBlock(roomNo)).toUpperCase(Locale.ROOT);
        }
        return new StudentImportRow(cell(r, 0), cell(r, 1), cell(r, 2), cell(r, 3), cell(r, 4), cell(r, 5), block, cleanRoomNumber(roomNo), "GENERAL");
    }

    private int findStudentHeaderRow(List<List<String>> rows) {
        for (int i = 0; i < Math.min(rows.size(), 25); i++) {
            Map<String, Integer> map = buildColumnMap(rows.get(i));
            boolean hasName = map.containsKey("name") || map.containsKey("studentname") || map.containsKey("nameofstudents");
            boolean hasRoom = map.containsKey("roomnumber") || map.containsKey("roomno") || map.containsKey("room");
            boolean hasReg = map.containsKey("registrationno") || map.containsKey("registration") || map.containsKey("regno") || map.containsKey("sno") || map.containsKey("serial");
            if (hasName && (hasReg || hasRoom)) return i;
        }
        return -1;
    }

    private Map<String, Integer> buildColumnMap(List<String> row) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < row.size(); i++) {
            String key = normalizeHeader(cell(row, i));
            if (!key.isBlank()) map.putIfAbsent(key, i);
        }
        return map;
    }

    private String normalizeHeader(String h) {
        String s = h == null ? "" : h.toLowerCase(Locale.ROOT).trim();
        s = s.replace("student's", "student").replace("father's", "father");
        s = s.replace("registration no", "registrationno").replace("reg. no", "regno").replace("reg no", "regno");
        s = s.replace("room-number", "roomno").replace("room-no", "roomno").replace("room no", "roomno");
        s = s.replace("name of students", "nameofstudents").replace("name of student", "studentname");
        s = s.replace("serial no", "sno").replace("sr no", "sno");
        return s.replaceAll("[^a-z0-9]", "");
    }

    private String first(List<String> row, Map<String, Integer> columns, String... keys) {
        for (String key : keys) {
            Integer index = columns.get(key);
            if (index != null) {
                String value = cell(row, index);
                if (!value.isBlank()) return value;
            }
        }
        return "";
    }

    private String normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) return "0000000000";
        Matcher group = Pattern.compile("\\d{10,15}").matcher(raw);
        if (group.find()) return group.group();
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() > 15) return digits.substring(0, 15);
        if (digits.length() == 9) return "0" + digits;
        if (digits.length() >= 10) return digits;
        return "0000000000";
    }

    private String extractBlock(String roomRaw) {
        if (roomRaw == null) return "A";
        Matcher m = Pattern.compile("(?i)(?:^|/|\\s)([A-Z])(?:/|\\s|$)").matcher(roomRaw.trim());
        return m.find() ? m.group(1).toUpperCase(Locale.ROOT) : "A";
    }

    private String cleanRoomNumber(String roomRaw) {
        if (roomRaw == null) return "";
        String value = roomRaw.trim().replaceAll("\\s+", "");
        value = value.replaceAll("(?i)/[A-Z](?=/|$)", "");
        value = value.replaceAll("(?i)(^|/)(BLOCK)?[A-Z]BLOCK(?=/|$)", "$1");
        value = value.replaceAll("//+", "/").replaceAll("^/|/$", "");
        return value;
    }

    private Room findOrCreateRoom(String block, String roomNo) {
        String finalBlock = defaultText(block, "A").toUpperCase(Locale.ROOT);
        String finalRoomNo = baseRoomNumber(cleanRoomNumber(roomNo));
        return roomRepository.findByBlockAndRoomNumber(finalBlock, finalRoomNo).orElseGet(() -> {
            Room room = new Room();
            room.setBlock(finalBlock);
            room.setRoomNumber(finalRoomNo);
            room.setFloor(guessFloor(finalRoomNo));
            room.setCapacity(3);
            room.setCurrentOccupancy(0);
            return roomRepository.save(room);
        });
    }

    private String baseRoomNumber(String roomNo) {
        if (roomNo == null) return "";
        String value = roomNo.trim();
        Matcher m = Pattern.compile("^(\\d+)(?:/\\d+)+$").matcher(value);
        return m.find() ? m.group(1) : value;
    }

    private void normalizeExistingSplitRoomRecords() {
        roomRepository.findAll().forEach(room -> {
            String base = baseRoomNumber(room.getRoomNumber());
            if (!base.equals(room.getRoomNumber())) {
                roomRepository.findByBlockAndRoomNumber(room.getBlock(), base).orElseGet(() -> {
                    Room parent = new Room();
                    parent.setBlock(room.getBlock());
                    parent.setRoomNumber(base);
                    parent.setFloor(guessFloor(base));
                    parent.setCapacity(3);
                    parent.setCurrentOccupancy(0);
                    return roomRepository.save(parent);
                });
            }
        });
    }

    private int guessFloor(String roomNo) {
        Matcher m = Pattern.compile("^(\\d)").matcher(roomNo == null ? "" : roomNo);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 1;
    }

    private List<List<String>> readRows(MultipartFile file) throws Exception {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (name.endsWith(".xlsx") || name.endsWith(".xls")) return readExcel(file.getInputStream());
        if (name.endsWith(".pdf")) return readPdf(file.getInputStream());
        throw new RuntimeException("Only .xlsx, .xls and text-based .pdf files are supported");
    }

    private List<List<String>> readExcel(InputStream is) throws Exception {
        List<List<String>> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            for (Row row : sheet) {
                List<String> values = new ArrayList<>();
                for (int c = 0; c < Math.max(row.getLastCellNum(), 0); c++) values.add(formatter.formatCellValue(row.getCell(c)).trim());
                rows.add(values);
            }
        }
        return rows;
    }

    private List<List<String>> readPdf(InputStream is) throws Exception {
        List<List<String>> rows = new ArrayList<>();
        try (PDDocument document = PDDocument.load(is)) {
            String text = new PDFTextStripper().getText(document);
            for (String line : text.split("\\R")) {
                if (!line.isBlank()) rows.add(Arrays.stream(line.split(",")).map(String::trim).toList());
            }
        }
        return rows;
    }

    private boolean isBlankRow(List<String> r) { return r.stream().allMatch(v -> v == null || v.isBlank()); }

    private boolean looksLikeBlock(String value) {
        if (value == null || value.isBlank()) return false;
        String v = value.trim().toUpperCase(Locale.ROOT);
        return v.matches("[A-Z]") || v.matches("BLOCK\\s+[A-Z]") || v.matches("[A-Z]-?BLOCK");
    }

    private boolean looksLikeHeader(List<String> r) {
        String first = cell(r, 0).toLowerCase(Locale.ROOT);
        return first.contains("registration") || first.equals("regno") || first.equals("reg no") || first.equals("date");
    }

    private String cell(List<String> r, int i) { return i < r.size() && r.get(i) != null ? r.get(i).trim() : ""; }
    private String defaultText(String v, String fallback) { return v == null || v.isBlank() ? fallback : v.trim(); }

    private LocalDate parseDate(String v) {
        if (v == null || v.isBlank()) return LocalDate.now();
        List<DateTimeFormatter> fmts = List.of(DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("d/M/yyyy"), DateTimeFormatter.ofPattern("d-M-yyyy"));
        for (DateTimeFormatter f : fmts) {
            try { return LocalDate.parse(v.trim(), f); } catch (Exception ignored) {}
        }
        return LocalDate.now();
    }

    private Attendance.AttendanceStatus parseStatus(String s) {
        String v = s.trim().toUpperCase(Locale.ROOT);
        if (v.startsWith("P")) return Attendance.AttendanceStatus.PRESENT;
        if (v.startsWith("L")) return Attendance.AttendanceStatus.LEAVE;
        return Attendance.AttendanceStatus.ABSENT;
    }

    private Map<String, Object> result(int count, List<String> errors, String type) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("type", type);
        r.put("saved", count);
        r.put("errors", errors);
        return r;
    }

    private record StudentImportRow(String registrationNo, String name, String fatherName, String phone, String email,
                                    String address, String block, String roomNumber, String category) {}
}

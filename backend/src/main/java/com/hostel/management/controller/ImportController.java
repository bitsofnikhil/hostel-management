package com.hostel.management.controller;

import com.hostel.management.service.ImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/import")
public class ImportController {
    private final ImportService importService;

    public ImportController(ImportService importService) { this.importService = importService; }

    @PostMapping("/students")
    public ResponseEntity<Map<String, Object>> importStudents(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(importService.importStudents(file));
    }

    @PostMapping("/attendance")
    public ResponseEntity<Map<String, Object>> importAttendance(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(importService.importAttendance(file));
    }
}

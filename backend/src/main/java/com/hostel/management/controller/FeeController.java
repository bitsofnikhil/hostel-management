package com.hostel.management.controller;

import com.hostel.management.dto.FeeRequest;
import com.hostel.management.model.Fee;
import com.hostel.management.service.FeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fees")
public class FeeController {

    private final FeeService feeService;

    public FeeController(FeeService feeService) {
        this.feeService = feeService;
    }

    @GetMapping
    public ResponseEntity<List<Fee>> getAllFees(@RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            try {
                Fee.FeeStatus feeStatus = Fee.FeeStatus.valueOf(status.toUpperCase());
                return ResponseEntity.ok(feeService.getFeesByStatus(feeStatus));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.ok(feeService.getAllFees());
            }
        }
        return ResponseEntity.ok(feeService.getAllFees());
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Fee>> getFeesByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(feeService.getFeesByStudent(studentId));
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportFeesExcel() {
        byte[] data = feeService.exportFeesExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fee-audit.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @PostMapping
    public ResponseEntity<Fee> createFee(@Valid @RequestBody FeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feeService.createFee(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Fee> updateFee(@PathVariable Long id, @Valid @RequestBody FeeRequest request) {
        return ResponseEntity.ok(feeService.updateFee(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFee(@PathVariable Long id) {
        feeService.deleteFee(id);
        return ResponseEntity.noContent().build();
    }
}

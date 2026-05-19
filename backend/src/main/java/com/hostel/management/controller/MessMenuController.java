package com.hostel.management.controller;

import com.hostel.management.dto.MessMenuRequest;
import com.hostel.management.model.MessMenu;
import com.hostel.management.service.MessMenuService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mess")
public class MessMenuController {
    private final MessMenuService service;
    public MessMenuController(MessMenuService service) { this.service = service; }

    @GetMapping("/menu")
    public ResponseEntity<List<MessMenu>> getMenu(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.getMenu(from, to));
    }

    @GetMapping("/suggestion")
    public ResponseEntity<Map<String, String>> suggestion(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(service.seasonalSuggestion(date));
    }

    @PostMapping("/menu")
    public ResponseEntity<MessMenu> save(@Valid @RequestBody MessMenuRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveMenu(request));
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now();
        byte[] data = service.exportMenuPdf(from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mess-menu-" + start + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @DeleteMapping("/menu/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

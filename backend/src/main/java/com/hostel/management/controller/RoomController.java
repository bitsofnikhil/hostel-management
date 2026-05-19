package com.hostel.management.controller;

import com.hostel.management.dto.RoomRequest;
import com.hostel.management.model.Room;
import com.hostel.management.service.RoomService;
import com.hostel.management.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final StudentService studentService;

    public RoomController(RoomService roomService, StudentService studentService) {
        this.roomService = roomService;
        this.studentService = studentService;
    }

    @GetMapping
    public ResponseEntity<List<Room>> getAllRooms(@RequestParam(required = false) String block) {
        return ResponseEntity.ok(block == null || block.isBlank() ? roomService.getAllRooms() : roomService.getRoomsByBlock(block));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) { return ResponseEntity.ok(roomService.getRoomById(id)); }

    @PostMapping
    public ResponseEntity<Room> createRoom(@Valid @RequestBody RoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.createRoom(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Room> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return ResponseEntity.ok(roomService.updateRoom(id, request));
    }

    @PostMapping("/allocate/{studentId}")
    public ResponseEntity<Room> allocateStudent(@PathVariable Long studentId, @RequestParam(required = false) Long roomId) {
        return ResponseEntity.ok(roomService.allocateStudent(studentId, roomId, studentService));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}

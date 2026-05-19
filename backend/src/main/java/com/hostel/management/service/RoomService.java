package com.hostel.management.service;

import com.hostel.management.dto.RoomRequest;
import com.hostel.management.model.Room;
import com.hostel.management.repository.RoomRepository;
import com.hostel.management.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;

    public RoomService(RoomRepository roomRepository, StudentRepository studentRepository) {
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
    }

    public List<Room> getAllRooms() {
        normalizeSplitRoomRecords();
        refreshAllOccupancy();
        return sortedRooms(roomRepository.findAll()).stream()
                .filter(room -> !isSplitBedRoom(room.getRoomNumber()))
                .toList();
    }

    public Room getRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));
    }

    public Room getFirstAvailableRoom() {
        return roomRepository.findAvailableRooms().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No room has free bed. Create room or increase capacity."));
    }

    @Transactional
    public Room createRoom(RoomRequest request) {
        String block = cleanBlock(request.getBlock());
        if (roomRepository.existsByBlockAndRoomNumber(block, request.getRoomNumber())) {
            throw new RuntimeException("Room " + block + "-" + request.getRoomNumber() + " already exists");
        }
        Room room = new Room();
        room.setRoomNumber(request.getRoomNumber());
        room.setBlock(block);
        room.setFloor(request.getFloor());
        room.setCapacity(normalizeCapacity(request.getCapacity()));
        room.setCurrentOccupancy(0);
        return roomRepository.save(room);
    }

    @Transactional
    public Room updateRoom(Long id, RoomRequest request) {
        Room room = getRoomById(id);
        String block = cleanBlock(request.getBlock());
        boolean sameRoom = room.getRoomNumber().equals(request.getRoomNumber()) && block.equals(room.getBlock());
        if (!sameRoom && roomRepository.existsByBlockAndRoomNumber(block, request.getRoomNumber())) {
            throw new RuntimeException("Room " + block + "-" + request.getRoomNumber() + " already exists");
        }
        int occupants = studentRepository.countByRoomIdAndActiveTrue(room.getId());
        int newCapacity = normalizeCapacity(request.getCapacity());
        if (newCapacity < occupants) {
            throw new RuntimeException("Capacity cannot be lower than current occupants: " + occupants);
        }
        room.setRoomNumber(request.getRoomNumber());
        room.setBlock(block);
        room.setFloor(request.getFloor());
        room.setCapacity(normalizeCapacity(request.getCapacity()));
        room.setCurrentOccupancy(occupants);
        return roomRepository.save(room);
    }

    @Transactional
    public void deleteRoom(Long id) {
        Room room = getRoomById(id);
        long occupants = studentRepository.countByRoomIdAndActiveTrue(room.getId());
        if (occupants > 0) {
            throw new RuntimeException("Cannot delete room with active students");
        }
        roomRepository.delete(room);
    }

    @Transactional
    public Room allocateStudent(Long studentId, Long roomId, StudentService studentService) {
        var student = studentService.findStudentById(studentId);
        Long oldRoomId = student.getRoom() != null ? student.getRoom().getId() : null;
        Room room = roomId == null ? getFirstAvailableRoom() : getRoomById(roomId);
        int activeStudents = studentRepository.countByRoomIdAndActiveTrue(room.getId());
        boolean alreadyInRoom = student.getRoom() != null && student.getRoom().getId().equals(room.getId());
        if (!alreadyInRoom && activeStudents >= room.getCapacity()) {
            throw new RuntimeException("Room " + room.getRoomNumber() + " is full");
        }
        student.setRoom(room);
        studentRepository.save(student);
        if (oldRoomId != null) updateOccupancy(oldRoomId);
        updateOccupancy(room.getId());
        return room;
    }

    @Transactional
    public void updateOccupancy(Long roomId) {
        Room room = getRoomById(roomId);
        int count = studentRepository.countByRoomIdAndActiveTrue(room.getId());
        room.setCurrentOccupancy(count);
        roomRepository.save(room);
    }

    @Transactional
    public void refreshAllOccupancy() {
        roomRepository.findAll().forEach(room -> {
            int count = studentRepository.countByRoomIdAndActiveTrue(room.getId());
            if (room.getCurrentOccupancy() != count) {
                room.setCurrentOccupancy(count);
                roomRepository.save(room);
            }
        });
    }

    public List<Room> getRoomsByBlock(String block) {
        normalizeSplitRoomRecords();
        refreshAllOccupancy();
        if (block == null || block.isBlank()) return getAllRooms();
        return sortedRooms(roomRepository.findByBlockOrderByFloorAscRoomNumberAsc(cleanBlock(block))).stream()
                .filter(room -> !isSplitBedRoom(room.getRoomNumber()))
                .toList();
    }

    @Transactional
    public void normalizeSplitRoomRecords() {
        List<Room> rooms = new ArrayList<>(roomRepository.findAll());
        for (Room child : rooms) {
            if (!isSplitBedRoom(child.getRoomNumber())) continue;

            String baseRoomNo = baseRoomNumber(child.getRoomNumber());
            Room parent = roomRepository.findByBlockAndRoomNumber(child.getBlock(), baseRoomNo).orElseGet(() -> {
                Room room = new Room();
                room.setBlock(child.getBlock());
                room.setRoomNumber(baseRoomNo);
                room.setFloor(child.getFloor());
                room.setCapacity(3);
                room.setCurrentOccupancy(0);
                return roomRepository.save(room);
            });

            List<com.hostel.management.model.Student> linkedStudents = studentRepository.findByRoom(child);
            int finalCapacity = Math.max(3, studentRepository.countByRoomIdAndActiveTrue(parent.getId()) + linkedStudents.size());
            if (parent.getCapacity() < finalCapacity) {
                parent.setCapacity(finalCapacity);
                roomRepository.save(parent);
            }

            linkedStudents.forEach(student -> student.setRoom(parent));
            studentRepository.saveAll(linkedStudents);
            roomRepository.delete(child);
        }
    }

    private boolean isSplitBedRoom(String roomNumber) {
        return roomNumber != null && roomNumber.trim().matches("^\\d+(/\\d+)+$");
    }

    private String baseRoomNumber(String roomNumber) {
        if (roomNumber == null) return "";
        Matcher matcher = Pattern.compile("^(\\d+)(?:/\\d+)+$").matcher(roomNumber.trim());
        return matcher.find() ? matcher.group(1) : roomNumber.trim();
    }

    private List<Room> sortedRooms(List<Room> rooms) {
        return rooms.stream()
                .sorted(Comparator
                        .comparing(Room::getBlock, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingInt(Room::getFloor)
                        .thenComparingInt(room -> firstNumber(room.getRoomNumber()))
                        .thenComparing(Room::getRoomNumber, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private int firstNumber(String value) {
        if (value == null) return Integer.MAX_VALUE;
        Matcher matcher = Pattern.compile("\\d+").matcher(value);
        if (!matcher.find()) return Integer.MAX_VALUE;
        try {
            return Integer.parseInt(matcher.group());
        } catch (Exception ex) {
            return Integer.MAX_VALUE;
        }
    }

    private String cleanBlock(String block) {
        return block == null || block.isBlank() ? "A" : block.trim().toUpperCase();
    }

    private int normalizeCapacity(int capacity) {
        return capacity <= 0 ? 3 : capacity;
    }
}

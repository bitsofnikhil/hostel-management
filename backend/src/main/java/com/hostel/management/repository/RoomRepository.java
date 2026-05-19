package com.hostel.management.repository;

import com.hostel.management.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByRoomNumberOrderByBlockAscFloorAscRoomNumberAsc(String roomNumber);
    Optional<Room> findByBlockAndRoomNumber(String block, String roomNumber);
    boolean existsByBlockAndRoomNumber(String block, String roomNumber);
    List<Room> findAllByOrderByBlockAscFloorAscRoomNumberAsc();
    List<Room> findByBlockOrderByFloorAscRoomNumberAsc(String block);

    @Query("select r from Room r where upper(r.block) = upper(?1) and r.currentOccupancy < r.capacity order by r.floor asc, r.roomNumber asc")
    List<Room> findAvailableRoomsByBlock(String block);

    @Query("select r from Room r where r.currentOccupancy < r.capacity order by r.block asc, r.floor asc, r.roomNumber asc")
    List<Room> findAvailableRooms();
}

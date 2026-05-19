package com.hostel.management.config;

import com.hostel.management.model.Room;
import com.hostel.management.repository.RoomRepository;
import com.hostel.management.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository,
                                      RoomRepository roomRepository) {
        return args -> {
            if (roomRepository.count() == 0) {
                String[] blocks = {"A", "A", "A", "A", "A", "A", "B", "B", "B", "B", "B", "B"};
                String[] roomNumbers = {"101", "102", "103", "104", "105", "106", "201", "202", "203", "204", "205", "206"};
                int[] floors = {1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2};
                int[] capacities = {3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4};
                for (int i = 0; i < roomNumbers.length; i++) {
                    Room room = new Room();
                    room.setBlock(blocks[i]);
                    room.setRoomNumber(roomNumbers[i]);
                    room.setFloor(floors[i]);
                    room.setCapacity(capacities[i]);
                    room.setCurrentOccupancy(0);
                    roomRepository.save(room);
                }
            }
        };
    }

}

package com.hostel.management.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "rooms", uniqueConstraints = @UniqueConstraint(columnNames = {"block", "room_number"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;

    @Column(nullable = false, length = 50)
    private String block = "A";

    @Column(nullable = false)
    private int floor = 1;

    @Column(nullable = false)
    private int capacity = 3;

    @Column(nullable = false)
    private int currentOccupancy = 0;
}

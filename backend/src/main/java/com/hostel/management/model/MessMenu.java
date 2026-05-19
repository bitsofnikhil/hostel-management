package com.hostel.management.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "mess_menu", uniqueConstraints = @UniqueConstraint(columnNames = {"menu_date", "meal_type"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessMenu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "menu_date", nullable = false)
    private LocalDate menuDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Season season = Season.SUMMER;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 15)
    private MealType mealType = MealType.LUNCH;

    @Column(nullable = false, length = 1200)
    private String items;

    @Column(length = 500)
    private String vegetables;

    @Column(name = "meal_time", length = 50)
    private String mealTime;

    @Column(length = 500)
    private String notes;

    public enum Season { SUMMER, WINTER, MONSOON, SPRING, AUTUMN }
    public enum MealType { BREAKFAST, LUNCH, SNACKS, DINNER }
}

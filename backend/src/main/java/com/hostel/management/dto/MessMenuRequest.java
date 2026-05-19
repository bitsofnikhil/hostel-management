package com.hostel.management.dto;

import com.hostel.management.model.MessMenu;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MessMenuRequest {
    @NotNull
    private LocalDate menuDate;
    private MessMenu.Season season;
    @NotNull
    private MessMenu.MealType mealType;
    @NotBlank
    private String items;
    private String vegetables;
    private String mealTime;
    private String notes;
}

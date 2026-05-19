package com.hostel.management.repository;

import com.hostel.management.model.MessMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessMenuRepository extends JpaRepository<MessMenu, Long> {
    List<MessMenu> findByMenuDateOrderByMealTypeAsc(LocalDate menuDate);
    List<MessMenu> findByMenuDateBetweenOrderByMenuDateAscMealTypeAsc(LocalDate from, LocalDate to);
    Optional<MessMenu> findByMenuDateAndMealType(LocalDate menuDate, MessMenu.MealType mealType);
}

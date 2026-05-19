package com.hostel.management.dto;

import com.hostel.management.model.Complaint;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ComplaintRequest {
    private Long studentId;
    private Long roomId;
    private Complaint.ComplaintCategory category = Complaint.ComplaintCategory.HOSTEL;
    private LocalDate complaintDate;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    private String resolution;
}

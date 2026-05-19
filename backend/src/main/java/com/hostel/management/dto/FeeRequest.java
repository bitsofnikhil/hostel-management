package com.hostel.management.dto;

import com.hostel.management.model.Fee;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FeeRequest {
    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotBlank(message = "Fee type is required")
    private String feeType;

    private Fee.BillingPeriod billingPeriod = Fee.BillingPeriod.ANNUAL;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private LocalDate paidDate;

    private Fee.FeeStatus status = Fee.FeeStatus.UNPAID;

    private Boolean securityRefunded = false;

    private LocalDate refundDate;

    private String remarks;
}

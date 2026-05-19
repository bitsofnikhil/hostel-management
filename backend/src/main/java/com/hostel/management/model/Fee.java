package com.hostel.management.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fees")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Fee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 50)
    private String feeType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BillingPeriod billingPeriod = BillingPeriod.ANNUAL;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private FeeStatus status = FeeStatus.UNPAID;

    @Column(name = "security_refunded")
    private Boolean securityRefunded = false;

    @Column(name = "refund_date")
    private LocalDate refundDate;

    @Column(length = 255)
    private String remarks;

    public enum FeeStatus {
        PAID, UNPAID, PARTIAL
    }

    public enum BillingPeriod {
        ANNUAL, ONE_TIME, OTHER
    }
}

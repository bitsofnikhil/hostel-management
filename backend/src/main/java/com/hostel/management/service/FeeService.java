package com.hostel.management.service;

import com.hostel.management.dto.FeeRequest;
import com.hostel.management.model.Fee;
import com.hostel.management.model.Student;
import com.hostel.management.repository.FeeRepository;
import com.hostel.management.repository.StudentRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

@Service
public class FeeService {

    private final FeeRepository feeRepository;
    private final StudentRepository studentRepository;

    public FeeService(FeeRepository feeRepository, StudentRepository studentRepository) {
        this.feeRepository = feeRepository;
        this.studentRepository = studentRepository;
    }

    public List<Fee> getAllFees() {
        return feeRepository.findAll();
    }

    public List<Fee> getFeesByStudent(Long studentId) {
        Student student = findStudent(studentId);
        return feeRepository.findByStudentOrderByDueDateDesc(student);
    }

    public List<Fee> getFeesByStatus(Fee.FeeStatus status) {
        return feeRepository.findByStatus(status);
    }

    @Transactional
    public Fee createFee(FeeRequest request) {
        Student student = findStudent(request.getStudentId());
        Fee.BillingPeriod billingPeriod = request.getBillingPeriod() != null
                ? request.getBillingPeriod()
                : defaultBillingPeriod(request.getFeeType());

        // Avoid duplicate rows when the warden uses Add Fee again for the same student,
        // fee type, billing period, and due-year. It updates the existing audit item instead.
        Fee fee = findExistingFeeForSameYear(student, request.getFeeType(), billingPeriod, request.getDueDate());
        applyRequest(fee, student, request);
        return feeRepository.save(fee);
    }

    @Transactional
    public Fee updateFee(Long id, FeeRequest request) {
        Fee fee = feeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fee record not found: " + id));
        Student student = findStudent(request.getStudentId());
        applyRequest(fee, student, request);
        return feeRepository.save(fee);
    }

    @Transactional
    public void deleteFee(Long id) {
        Fee fee = feeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fee record not found: " + id));
        feeRepository.delete(fee);
    }

    private Fee findExistingFeeForSameYear(Student student, String feeType, Fee.BillingPeriod billingPeriod, LocalDate dueDate) {
        if (student == null || feeType == null || feeType.isBlank() || billingPeriod == null || dueDate == null) {
            return new Fee();
        }
        LocalDate yearStart = LocalDate.of(dueDate.getYear(), 1, 1);
        LocalDate yearEnd = LocalDate.of(dueDate.getYear(), 12, 31);
        List<Fee> matches = feeRepository.findPossibleDuplicates(student, feeType, billingPeriod, yearStart, yearEnd);
        return matches.isEmpty() ? new Fee() : matches.get(0);
    }

    private void applyRequest(Fee fee, Student student, FeeRequest request) {
        fee.setStudent(student);
        fee.setFeeType(request.getFeeType());
        fee.setBillingPeriod(request.getBillingPeriod() != null ? request.getBillingPeriod() : defaultBillingPeriod(request.getFeeType()));
        fee.setAmount(request.getAmount());
        fee.setDueDate(request.getDueDate());
        fee.setPaidDate(request.getPaidDate());
        fee.setStatus(request.getStatus() != null ? request.getStatus() : Fee.FeeStatus.UNPAID);
        fee.setSecurityRefunded(Boolean.TRUE.equals(request.getSecurityRefunded()));
        fee.setRefundDate(request.getRefundDate());
        fee.setRemarks(request.getRemarks());
    }

    public byte[] exportFeesExcel() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Fee Audit");
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            String[] headers = {"Student Name", "Registration No", "Category", "Room", "Fee Type", "Billing Period", "Amount", "Due Date", "Paid Date", "Status", "Security Refunded", "Refund Date", "Remarks"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            List<Fee> fees = feeRepository.findAll();
            int rowNum = 1;
            for (Fee fee : fees) {
                Row row = sheet.createRow(rowNum++);
                Student s = fee.getStudent();
                row.createCell(0).setCellValue(s != null ? s.getName() : "");
                row.createCell(1).setCellValue(s != null ? s.getRegistrationNo() : "");
                row.createCell(2).setCellValue(s != null ? s.getCategory() : "");
                row.createCell(3).setCellValue(s != null && s.getRoom() != null ? s.getRoom().getRoomNumber() : "");
                row.createCell(4).setCellValue(nullSafe(fee.getFeeType()));
                row.createCell(5).setCellValue(fee.getBillingPeriod() != null ? fee.getBillingPeriod().name() : "");
                row.createCell(6).setCellValue(fee.getAmount() != null ? fee.getAmount().doubleValue() : 0);
                row.createCell(7).setCellValue(dateText(fee.getDueDate()));
                row.createCell(8).setCellValue(dateText(fee.getPaidDate()));
                row.createCell(9).setCellValue(fee.getStatus() != null ? fee.getStatus().name() : "");
                row.createCell(10).setCellValue(Boolean.TRUE.equals(fee.getSecurityRefunded()) ? "YES" : "NO");
                row.createCell(11).setCellValue(dateText(fee.getRefundDate()));
                row.createCell(12).setCellValue(nullSafe(fee.getRemarks()));
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Could not export fee audit Excel", e);
        }
    }

    private Fee.BillingPeriod defaultBillingPeriod(String feeType) {
        if (feeType == null) return Fee.BillingPeriod.ANNUAL;
        String t = feeType.toLowerCase();
        if (t.contains("security")) return Fee.BillingPeriod.ONE_TIME;
        if (t.contains("rent") || t.contains("mess")) return Fee.BillingPeriod.ANNUAL;
        return Fee.BillingPeriod.OTHER;
    }

    private String dateText(LocalDate d) { return d == null ? "" : d.toString(); }
    private String nullSafe(String v) { return v == null ? "" : v; }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found: " + id));
    }
}

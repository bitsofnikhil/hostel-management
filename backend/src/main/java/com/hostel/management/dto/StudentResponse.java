package com.hostel.management.dto;

import com.hostel.management.model.Student;
import lombok.Data;

@Data
public class StudentResponse {
    private Long id;
    private String registrationNo;
    private String name;
    private String fatherName;
    private String phone;
    private String email;
    private String address;
    private Long roomId;
    private String roomNumber;
    private String category;
    private Integer importOrder;
    private boolean active;

    public static StudentResponse fromStudent(Student s) {
        StudentResponse r = new StudentResponse();
        r.setId(s.getId());
        r.setRegistrationNo(s.getRegistrationNo());
        r.setName(s.getName());
        r.setFatherName(s.getFatherName());
        r.setPhone(s.getPhone());
        r.setEmail(s.getEmail());
        r.setAddress(s.getAddress());
        r.setCategory(s.getCategory());
        r.setImportOrder(s.getImportOrder());
        r.setActive(s.isActive());
        if (s.getRoom() != null) {
            r.setRoomId(s.getRoom().getId());
            r.setRoomNumber(s.getRoom().getRoomNumber());
        }
        return r;
    }
}

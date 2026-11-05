package com.abittech.platform.student.entity;

import com.abittech.platform.common.persistence.AuditedEntity;
import com.abittech.platform.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Student-specific information of a user account (at most one per user, enforced by the database).
 * Name, e-mail, password and role stay in {@link User}.
 * <p>
 * {@code university} and {@code programme} are plain text for now; they can become normalised
 * entities later if the platform needs university management.
 */
@Entity
@Table(name = "student_profiles")
public class StudentProfile extends AuditedEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "university", nullable = false, length = 200)
    private String university;

    @Column(name = "programme", nullable = false, length = 200)
    private String programme;

    @Column(name = "year_of_study")
    private Integer yearOfStudy;

    @Column(name = "student_number", length = 50)
    private String studentNumber;

    @Column(name = "phone", length = 30)
    private String phone;

    protected StudentProfile() {
        // required by JPA
    }

    public StudentProfile(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getProgramme() {
        return programme;
    }

    public void setProgramme(String programme) {
        this.programme = programme;
    }

    public Integer getYearOfStudy() {
        return yearOfStudy;
    }

    public void setYearOfStudy(Integer yearOfStudy) {
        this.yearOfStudy = yearOfStudy;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}

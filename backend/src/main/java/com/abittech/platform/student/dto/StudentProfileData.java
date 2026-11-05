package com.abittech.platform.student.dto;

/**
 * The editable fields of a student profile, shared by the create and update requests so that
 * the service applies them in one place.
 */
public interface StudentProfileData {

    String university();

    String programme();

    Integer yearOfStudy();

    String studentNumber();

    String phone();
}

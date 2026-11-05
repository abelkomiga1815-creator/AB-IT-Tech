package com.abittech.platform.project.dto;

import java.time.LocalDate;

/**
 * The editable fields shared by the create and update requests.
 */
public interface ProjectData {

    String title();

    String description();

    String researchArea();

    String supervisorName();

    LocalDate deadline();
}

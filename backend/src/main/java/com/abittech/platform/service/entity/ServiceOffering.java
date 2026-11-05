package com.abittech.platform.service.entity;

import com.abittech.platform.common.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * One technology service offered by AB IT TECH SOLUTIONS (software development, web development, ...).
 * Named {@code ServiceOffering} to avoid confusion with Spring's {@code @Service} classes.
 */
@Entity
@Table(name = "services")
public class ServiceOffering extends AuditedEntity {

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "summary", nullable = false, length = 300)
    private String summary;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected ServiceOffering() {
        // required by JPA
    }

    public ServiceOffering(String slug, String name, String summary, String description, int displayOrder) {
        this.slug = slug;
        this.name = name;
        this.summary = summary;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

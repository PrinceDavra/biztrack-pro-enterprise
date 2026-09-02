package com.biztrackpro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Configurable thresholds used by the AI Advisor (e.g. margin/AOV/ROAS cut-offs),
 * so business rules can be tuned without a redeploy.
 */
@Entity
@Table(name = "system_properties",
        uniqueConstraints = @UniqueConstraint(name = "uq_sysprop_key", columnNames = "prop_key"))
public class SystemProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prop_key", length = 100, nullable = false, unique = true)
    private String propKey;

    @Column(name = "prop_value", length = 255)
    private String propValue;

    @Column(name = "description", length = 255)
    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPropKey() {
        return propKey;
    }

    public void setPropKey(String propKey) {
        this.propKey = propKey;
    }

    public String getPropValue() {
        return propValue;
    }

    public void setPropValue(String propValue) {
        this.propValue = propValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

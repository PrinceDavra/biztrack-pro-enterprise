package com.biztrackpro.dto;

/**
 * Business profile — used both as the GET /profile response and the PUT /profile body.
 */
public class ProfileDTO {

    public String businessName;
    public String gstin;
    public String financialYear;
    public String caName;

    public ProfileDTO() {
    }

    public ProfileDTO(String businessName, String gstin, String financialYear, String caName) {
        this.businessName = businessName;
        this.gstin = gstin;
        this.financialYear = financialYear;
        this.caName = caName;
    }
}

package com.biztrackpro.security;

import jakarta.enterprise.context.RequestScoped;

/**
 * Holds the authenticated tenant for the current request. Populated by
 * {@code JwtAuthFilter} and read by resources so a tenant only ever sees its own data.
 */
@RequestScoped
public class AuthPrincipal {

    private Long tenantId;
    private String email;

    public boolean isAuthenticated() {
        return tenantId != null;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

package com.biztrackpro.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.mindrot.jbcrypt.BCrypt;

import com.biztrackpro.dto.Requests.LoginRequest;
import com.biztrackpro.dto.Requests.RegisterRequest;
import com.biztrackpro.entity.BusinessProfile;
import com.biztrackpro.entity.Tenant;
import com.biztrackpro.repository.BusinessProfileRepository;
import com.biztrackpro.repository.TenantRepository;
import com.biztrackpro.security.JwtUtil;
import com.biztrackpro.util.Display;
import com.biztrackpro.util.Tx;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Tenant registration and login. Passwords are hashed with BCrypt; a JWT is
 * issued on success. Never logs passwords or tokens.
 */
@ApplicationScoped
public class AuthService {

    @Inject
    private TenantRepository tenantRepo;

    @Inject
    private BusinessProfileRepository profileRepo;

    @Inject
    private JwtUtil jwtUtil;

    @Inject
    private EntityManager em;

    public Map<String, Object> register(RegisterRequest req) {
        if (req == null || req.email == null || req.email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (req.password == null || req.password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        final String email = req.email.trim().toLowerCase();
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Please enter a valid email address");
        }
        if (tenantRepo.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }
        final String name = (req.name == null || req.name.isBlank()) ? email : req.name.trim();

        Tenant tenant = Tx.call(em, () -> {
            Tenant t = new Tenant();
            t.setName(name);
            t.setEmail(email);
            t.setPasswordHash(BCrypt.hashpw(req.password, BCrypt.gensalt(12)));
            tenantRepo.save(t);

            BusinessProfile profile = new BusinessProfile();
            profile.setTenantId(t.getId());
            profile.setBusinessName(name);
            profile.setFinancialYear(Display.currentFinancialYear());
            profileRepo.save(profile);
            return t;
        });

        String token = jwtUtil.generateToken(tenant.getId(), tenant.getEmail());
        return payload(tenant, token);
    }

    public Map<String, Object> login(LoginRequest req) {
        if (req == null || req.email == null || req.password == null) {
            throw new SecurityException("Invalid email or password");
        }
        final String email = req.email.trim().toLowerCase();
        Tenant tenant = tenantRepo.findByEmail(email)
                .orElseThrow(() -> new SecurityException("Invalid email or password"));

        if (tenant.getPasswordHash() == null || !BCrypt.checkpw(req.password, tenant.getPasswordHash())) {
            throw new SecurityException("Invalid email or password");
        }
        String token = jwtUtil.generateToken(tenant.getId(), tenant.getEmail());
        return payload(tenant, token);
    }

    private Map<String, Object> payload(Tenant tenant, String token) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("token", token);
        m.put("tenantId", tenant.getId());
        m.put("name", tenant.getName());
        m.put("email", tenant.getEmail());
        return m;
    }
}

package com.biztrackpro.service;

import com.biztrackpro.dto.ProfileDTO;
import com.biztrackpro.entity.BusinessProfile;
import com.biztrackpro.repository.BusinessProfileRepository;
import com.biztrackpro.util.Display;
import com.biztrackpro.util.Tx;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Business profile read/update. The profile drives the CA export headers
 * (business name, GSTIN, financial year, CA name).
 */
@ApplicationScoped
public class ProfileService {

    @Inject
    private BusinessProfileRepository profileRepo;

    @Inject
    private EntityManager em;

    public ProfileDTO get(Long tenantId) {
        return profileRepo.findByTenant(tenantId)
                .map(p -> new ProfileDTO(p.getBusinessName(), p.getGstin(), p.getFinancialYear(), p.getCaName()))
                .orElseGet(() -> new ProfileDTO("", "", Display.currentFinancialYear(), ""));
    }

    /** Full entity — used by ExportService for filenames and header rows. */
    public BusinessProfile getEntity(Long tenantId) {
        return profileRepo.findByTenant(tenantId).orElseGet(() -> {
            BusinessProfile p = new BusinessProfile();
            p.setTenantId(tenantId);
            p.setBusinessName("BizTrack Pro");
            p.setFinancialYear(Display.currentFinancialYear());
            return p;
        });
    }

    public ProfileDTO update(Long tenantId, ProfileDTO dto) {
        return Tx.call(em, () -> {
            BusinessProfile p = profileRepo.findByTenant(tenantId).orElseGet(BusinessProfile::new);
            p.setTenantId(tenantId);
            p.setBusinessName(dto.businessName);
            p.setGstin(dto.gstin);
            p.setFinancialYear(dto.financialYear != null && !dto.financialYear.isBlank()
                    ? dto.financialYear : Display.currentFinancialYear());
            p.setCaName(dto.caName);
            profileRepo.save(p);
            return new ProfileDTO(p.getBusinessName(), p.getGstin(), p.getFinancialYear(), p.getCaName());
        });
    }
}

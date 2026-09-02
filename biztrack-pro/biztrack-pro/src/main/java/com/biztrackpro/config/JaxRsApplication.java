package com.biztrackpro.config;

import java.util.HashSet;
import java.util.Set;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import com.biztrackpro.filter.JwtAuthFilter;
import com.biztrackpro.resource.AdsResource;
import com.biztrackpro.resource.AdvisorResource;
import com.biztrackpro.resource.AnalyticsResource;
import com.biztrackpro.resource.AuthResource;
import com.biztrackpro.resource.CostsResource;
import com.biztrackpro.resource.DashboardResource;
import com.biztrackpro.resource.ExpenseResource;
import com.biztrackpro.resource.ExportResource;
import com.biztrackpro.resource.ImportResource;
import com.biztrackpro.resource.ProfileResource;
import com.biztrackpro.resource.SalesResource;

import jakarta.ws.rs.core.Application;

/**
 * JAX-RS application. The base path (/api) is defined by the servlet-mapping in web.xml,
 * so no @ApplicationPath is declared here to avoid a doubled path segment.
 */
public class JaxRsApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        // Resources (Controllers)
        classes.add(AuthResource.class);
        classes.add(ImportResource.class);
        classes.add(DashboardResource.class);
        classes.add(SalesResource.class);
        classes.add(ExpenseResource.class);
        classes.add(AdsResource.class);
        classes.add(CostsResource.class);
        classes.add(AnalyticsResource.class);
        classes.add(AdvisorResource.class);
        classes.add(ExportResource.class);
        classes.add(ProfileResource.class);

        // Providers / features
        classes.add(JwtAuthFilter.class);
        classes.add(CorsFilter.class);
        classes.add(AppExceptionMapper.class);
        classes.add(ObjectMapperContextResolver.class);
        classes.add(JacksonFeature.class);
        classes.add(MultiPartFeature.class);

        return classes;
    }
}

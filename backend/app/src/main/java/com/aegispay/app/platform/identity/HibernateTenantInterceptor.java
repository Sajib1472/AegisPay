package com.aegispay.app.platform.identity;

import com.aegispay.app.platform.tenancy.TenantContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class HibernateTenantInterceptor implements HandlerInterceptor {

    private final EntityManager entityManager;

    public HibernateTenantInterceptor(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (TenantContext.tenantId() != null) {
            entityManager.unwrap(Session.class)
                    .enableFilter("tenantFilter")
                    .setParameter("tenantId", TenantContext.tenantId());
        }
        return true;
    }
}

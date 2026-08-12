package com.saas.school.security;

import com.saas.school.config.SecurityUtil;
import com.saas.school.entity.HasPermission;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final SecurityUtil securityUtil;

    @Around("@annotation(hasPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint,
                                  HasPermission hasPermission) throws Throwable {

        String requiredPermission = hasPermission.value();

        List<String> userPermissions = securityUtil.getCurrentPermissions();
        System.out.println("SECURITY CONTEXT = " + SecurityContextHolder.getContext().getAuthentication());

        System.out.println("🔍 REQUIRED PERMISSION = " + requiredPermission);
        System.out.println("🔍 USER PERMISSIONS = " + userPermissions);

        if (userPermissions.contains(requiredPermission)) {
            return joinPoint.proceed();
        }

        throw new RuntimeException("❌ Accès refusé : permission manquante");
    }
}
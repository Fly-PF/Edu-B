package com.edu.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SafetyControllerSecurityContractTests {
    private static final String GATEWAY = "hasAnyAuthority('ADMIN','SUPERADMIN','TEACHER','STUDENT')";
    private static final String STAFF = "hasAnyAuthority('ADMIN','SUPERADMIN','TEACHER')";
    private static final String ADMIN = "hasAnyAuthority('ADMIN','SUPERADMIN')";

    @Test
    void shouldSeparateBusinessGatewayFromStaffSafetyTools() {
        assertPreAuthorize("check", STAFF);
        assertPreAuthorize("gateway", GATEWAY);
        assertPreAuthorize("checkEvidence", STAFF);
    }

    @Test
    void shouldRestrictGlobalSafetyGovernanceToAdmins() {
        assertPreAuthorize("runEvaluation", ADMIN);
        assertPreAuthorize("dashboard", ADMIN);
        assertPreAuthorize("pageRecords", ADMIN);
        assertPreAuthorize("recordDetail", ADMIN);
    }

    private void assertPreAuthorize(String methodName, String expected) {
        Method method = Arrays.stream(SafetyController.class.getDeclaredMethods())
                .filter(item -> item.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertNotNull(annotation);
        assertEquals(expected, annotation.value());
    }
}

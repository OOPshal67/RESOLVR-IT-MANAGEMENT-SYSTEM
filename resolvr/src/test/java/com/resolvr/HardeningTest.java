package com.resolvr;

import com.resolvr.dto.*;
import com.resolvr.entity.*;
import com.resolvr.enums.Role;
import com.resolvr.exception.*;
import com.resolvr.repository.IncidentRepository;
import com.resolvr.service.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HardeningTest {
    @Test
    void invalidNumbersReachTheValidator() {
        var incident = new IncidentCreateRequest();
        incident.setAffectedUsers(0);
        var resolution = new ResolutionRequest();
        resolution.setTimeSpentMinutes(-1);
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var validator = validators.getValidator();
            assertFalse(validator.validateProperty(incident, "affectedUsers").isEmpty());
            assertFalse(validator.validateProperty(resolution, "timeSpentMinutes").isEmpty());
        }
    }

    @Test
    void passwordLimitCountsUtf8Bytes() {
        String boundary = "é".repeat(36);
        assertEquals(72, boundary.getBytes(StandardCharsets.UTF_8).length);
        assertDoesNotThrow(() -> AccountAccessService.requirePassword(boundary));
        assertThrows(InvalidIncidentException.class, () -> AccountAccessService.requirePassword(boundary + "a"));
        assertThrows(InvalidIncidentException.class, () -> AccountAccessService.requirePassword(null));
    }

    @Test
    void technicianCanOnlyReadAssignedTickets() {
        var service = mock(IncidentService.class, CALLS_REAL_METHODS);
        var incident = new Incident();
        var tech = mock(Technician.class);
        when(tech.getRole()).thenReturn(Role.ROLE_TECHNICIAN);
        when(tech.getId()).thenReturn(12L);
        assertThrows(UnauthorizedOperationException.class, () -> service.verifyViewPermission(incident, tech));
        assertThrows(UnauthorizedOperationException.class, () -> service.verifyViewPermission(incident, null));
        incident.setAssignedTechnician(tech);
        assertDoesNotThrow(() -> service.verifyViewPermission(incident, tech));
    }

    @Test
    void noResolvedTicketsHasZeroAverage() {
        var incidents = mock(IncidentRepository.class);
        when(incidents.readMetrics()).thenReturn(List.of());
        var stats = new AnalyticsService(incidents, null, null).computeDashboardStats();
        assertEquals(0.0, stats.getAverageResolutionMinutes());
        verify(incidents, never()).findAll();
    }

    @Test
    void missingDatesAndPolicyHaveNoElapsedProgress() {
        var incident = new Incident();
        incident.setCreatedAt(null);
        assertEquals(0, incident.getElapsedMinutes());
        assertEquals(0, incident.getSlaProgressPercentage());
    }

    @Test
    void exportQuotesSpreadsheetFormulas() {
        var incidents = mock(IncidentRepository.class);
        var incident = new Incident();
        incident.setTitle("=1+2");
        incident.setPriority(com.resolvr.enums.PriorityLevel.P4);
        incident.setStatus(com.resolvr.enums.IncidentStatus.NEW);
        when(incidents.findAll()).thenReturn(List.of(incident));
        assertTrue(new AnalyticsService(incidents, null, null).generateCsvExport().contains("\"'=1+2\""));
    }
}

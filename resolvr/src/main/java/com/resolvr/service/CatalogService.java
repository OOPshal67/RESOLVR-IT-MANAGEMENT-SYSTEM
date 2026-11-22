package com.resolvr.service;

import com.resolvr.entity.ApplicationConfiguration;
import com.resolvr.entity.ITService;
import com.resolvr.entity.IncidentCategory;
import com.resolvr.entity.SLAPolicy;
import com.resolvr.enums.BusinessCriticality;
import com.resolvr.enums.PriorityLevel;
import com.resolvr.exception.InvalidIncidentException;
import com.resolvr.exception.MissingSLAPolicyException;
import com.resolvr.exception.ServiceNotFoundException;
import com.resolvr.repository.ApplicationConfigurationRepository;
import com.resolvr.repository.ITServiceRepository;
import com.resolvr.repository.IncidentCategoryRepository;
import com.resolvr.repository.SLAPolicyRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CatalogService {

    public static final String CONFIG_ASSIGNMENT_STRATEGY = "ACTIVE_ASSIGNMENT_STRATEGY";
    public static final String CONFIG_PRIORITY_STRATEGY = "ACTIVE_PRIORITY_STRATEGY";
    public static final String CONFIG_ESCALATION_ENABLED = "AUTOMATIC_ESCALATION_ENABLED";

    private final IncidentCategoryRepository categoryRepository;
    private final ITServiceRepository serviceRepository;
    private final SLAPolicyRepository slaPolicyRepository;
    private final ApplicationConfigurationRepository configRepository;
    private final AuditService auditService;

    public CatalogService(IncidentCategoryRepository categoryRepository,
                          ITServiceRepository serviceRepository,
                          SLAPolicyRepository slaPolicyRepository,
                          ApplicationConfigurationRepository configRepository,
                          AuditService auditService) {
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.configRepository = configRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<IncidentCategory> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<IncidentCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public IncidentCategory getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ServiceNotFoundException("Incident category not found with ID: " + id));
    }

    public IncidentCategory createCategory(String code, String name, String description, double defaultWeight, String actor) {
        IncidentCategory category = new IncidentCategory(code.trim().toUpperCase(), name.trim(), description, defaultWeight);
        IncidentCategory saved = categoryRepository.save(category);
        auditService.recordSystemAction(actor, "ROLE_ADMIN", "CATEGORY_CREATED",
                "Created incident category " + saved.getCode() + " (" + saved.getName() + ")", "Weight=" + defaultWeight);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ITService> getActiveServices() {
        return serviceRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<ITService> getAllServices() {
        return serviceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ITService getServiceById(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ServiceNotFoundException("IT Service not found with ID: " + id));
    }

    public ITService createITService(String serviceCode, String name, String description,
                                     BusinessCriticality criticality, String ownerDepartment,
                                     String operationalStatus, String actor) {
        ITService service = new ITService(serviceCode.trim().toUpperCase(), name.trim(), description,
                criticality, ownerDepartment, operationalStatus != null ? operationalStatus : "OPERATIONAL");
        ITService saved = serviceRepository.save(service);
        auditService.recordSystemAction(actor, "ROLE_ADMIN", "SERVICE_CREATED",
                "Added IT service " + saved.getName() + " [" + saved.getServiceCode() + "]",
                "Criticality=" + criticality);
        return saved;
    }

    public void updateServiceStatus(Long serviceId, String operationalStatus, BusinessCriticality criticality, String actor) {
        ITService service = getServiceById(serviceId);
        service.setOperationalStatus(operationalStatus);
        if (criticality != null) {
            service.setBusinessCriticality(criticality);
        }
        serviceRepository.save(service);
        auditService.recordSystemAction(actor, "ROLE_ADMIN", "SERVICE_UPDATED",
                "Updated IT service " + service.getName() + " status to " + operationalStatus, null);
    }

    @Transactional(readOnly = true)
    public List<SLAPolicy> getAllSLAPolicies() {
        return slaPolicyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public SLAPolicy getSLAPolicyForPriority(PriorityLevel priorityLevel) {
        return slaPolicyRepository.findByPriorityLevel(priorityLevel)
                .orElseThrow(() -> new MissingSLAPolicyException("No SLA policy configured for priority " + priorityLevel));
    }

    public SLAPolicy updateSLAPolicy(Long policyId, int responseMinutes, int resolutionMinutes,
                                     int warningThresholdPercent, boolean escalationEnabled, String actor) {
        SLAPolicy policy = slaPolicyRepository.findById(policyId)
                .orElseThrow(() -> new MissingSLAPolicyException("SLA Policy not found with ID: " + policyId));
        if (resolutionMinutes < responseMinutes || responseMinutes < 1) {
            throw new InvalidIncidentException("Resolution time must be at least the response time.");
        }
        policy.setResponseTargetMinutes(Math.max(1, responseMinutes));
        policy.setResolutionTargetMinutes(Math.max(responseMinutes, resolutionMinutes));
        policy.setWarningThresholdPercent(Math.max(5, Math.min(90, warningThresholdPercent)));
        policy.setEscalationEnabled(escalationEnabled);
        policy.setUpdatedAt(LocalDateTime.now());
        SLAPolicy saved = slaPolicyRepository.save(policy);
        auditService.recordSystemAction(actor, "ROLE_ADMIN", "SLA_POLICY_UPDATED",
                "Updated SLA Policy for " + saved.getPriorityLevel() +
                " (Response=" + responseMinutes + "m, Resolution=" + resolutionMinutes + "m)", null);
        return saved;
    }

    @Transactional(readOnly = true)
    public String getConfigValue(String key, String defaultValue) {
        return configRepository.findByConfigKey(key)
                .map(ApplicationConfiguration::getConfigValue)
                .orElse(defaultValue);
    }

    public void updateConfigValue(String key, String value, String description, String actor) {
        ApplicationConfiguration config = configRepository.findByConfigKey(key)
                .orElseGet(() -> new ApplicationConfiguration(key, value, description, actor));
        config.setConfigValue(value);
        if (description != null) {
            config.setDescription(description);
        }
        config.setUpdatedBy(actor);
        config.setUpdatedAt(LocalDateTime.now());
        configRepository.save(config);
        auditService.recordSystemAction(actor, "ROLE_ADMIN", "CONFIG_UPDATED",
                "Updated configuration " + key + " = " + value, description);
    }

    @Transactional(readOnly = true)
    public List<ApplicationConfiguration> getAllConfigurations() {
        return configRepository.findAll();
    }
}

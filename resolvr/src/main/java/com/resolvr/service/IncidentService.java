package com.resolvr.service;

import com.resolvr.dto.*;
import com.resolvr.entity.*;
import com.resolvr.enums.*;
import com.resolvr.exception.*;
import com.resolvr.factory.IncidentFactory;
import com.resolvr.observer.IncidentDomainEvent;
import com.resolvr.observer.IncidentEventPublisher;
import com.resolvr.repository.*;
import com.resolvr.state.IncidentStateMachine;
import com.resolvr.strategy.PriorityCalculationStrategy;
import com.resolvr.strategy.TechnicianAssignmentStrategy;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final AssignmentRepository assignmentRepository;
    private final InvestigationNoteRepository noteRepository;
    private final InformationRequestRepository infoRequestRepository;
    private final ResolutionRepository resolutionRepository;
    private final IncidentFeedbackRepository feedbackRepository;
    private final TechnicianRepository technicianRepository;
    private final AdministratorRepository administratorRepository;
    private final CatalogService catalogService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final SLARiskDetectionService slaRiskDetectionService;
    private final IncidentFactory incidentFactory;
    private final IncidentStateMachine stateMachine;
    private final IncidentEventPublisher eventPublisher;
    private final Map<String, PriorityCalculationStrategy> priorityStrategies = new HashMap<>();
    private final Map<String, TechnicianAssignmentStrategy> assignmentStrategies = new HashMap<>();

    public IncidentService(IncidentRepository incidentRepository,
                           AssignmentRepository assignmentRepository,
                           InvestigationNoteRepository noteRepository,
                           InformationRequestRepository infoRequestRepository,
                           ResolutionRepository resolutionRepository,
                           IncidentFeedbackRepository feedbackRepository,
                           TechnicianRepository technicianRepository,
                           AdministratorRepository administratorRepository,
                           CatalogService catalogService,
                           DuplicateDetectionService duplicateDetectionService,
                           SLARiskDetectionService slaRiskDetectionService,
                           IncidentFactory incidentFactory,
                           IncidentStateMachine stateMachine,
                           IncidentEventPublisher eventPublisher,
                           List<PriorityCalculationStrategy> priorityStrategyList,
                           List<TechnicianAssignmentStrategy> assignmentStrategyList) {
        this.incidentRepository = incidentRepository;
        this.assignmentRepository = assignmentRepository;
        this.noteRepository = noteRepository;
        this.infoRequestRepository = infoRequestRepository;
        this.resolutionRepository = resolutionRepository;
        this.feedbackRepository = feedbackRepository;
        this.technicianRepository = technicianRepository;
        this.administratorRepository = administratorRepository;
        this.catalogService = catalogService;
        this.duplicateDetectionService = duplicateDetectionService;
        this.slaRiskDetectionService = slaRiskDetectionService;
        this.incidentFactory = incidentFactory;
        this.stateMachine = stateMachine;
        this.eventPublisher = eventPublisher;

        for (PriorityCalculationStrategy s : priorityStrategyList) {
            this.priorityStrategies.put(s.getStrategyCode(), s);
        }
        for (TechnicianAssignmentStrategy s : assignmentStrategyList) {
            this.assignmentStrategies.put(s.getStrategyCode(), s);
        }
    }

    public PriorityCalculationStrategy getActivePriorityStrategy() {
        String code = catalogService.getConfigValue(CatalogService.CONFIG_PRIORITY_STRATEGY, "WEIGHTED_SCORE");
        return priorityStrategies.getOrDefault(code, priorityStrategies.values().iterator().next());
    }

    public TechnicianAssignmentStrategy getActiveAssignmentStrategy() {
        String code = catalogService.getConfigValue(CatalogService.CONFIG_ASSIGNMENT_STRATEGY, "SKILL_BASED");
        return assignmentStrategies.getOrDefault(code, assignmentStrategies.values().iterator().next());
    }

    public Collection<PriorityCalculationStrategy> getAllPriorityStrategies() {
        return priorityStrategies.values();
    }

    public Collection<TechnicianAssignmentStrategy> getAllAssignmentStrategies() {
        return assignmentStrategies.values();
    }

    public String generateNextIncidentKey() {
        return "INC" + incidentRepository.nextIncidentNumber();
    }

    public Incident createIncident(IncidentCreateRequest request, Employee reporter) {
        if (request == null || reporter == null) throw new InvalidIncidentException("A reporter and incident details are required.");
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new InvalidIncidentException("Incident title cannot be empty.");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new InvalidIncidentException("Incident description cannot be empty.");
        }

        IncidentCategory category = catalogService.getCategoryById(request.getCategoryId());
        ITService service = catalogService.getServiceById(request.getServiceId());
        PriorityCalculationStrategy priorityStrategy = getActivePriorityStrategy();

        PriorityResult priorityResult = priorityStrategy.calculatePriority(
            request.getImpact(),
            request.getUrgency(),
            service.getBusinessCriticality(),
            request.getAffectedUsers(),
            category,
            service
        );

        SLAPolicy slaPolicy = catalogService.getSLAPolicyForPriority(priorityResult.getPriorityLevel());
        String incidentKey = generateNextIncidentKey();

        Incident incident = incidentFactory.createIncident(
            incidentKey, request, reporter, category, service, priorityResult, slaPolicy
        );

        Incident saved = incidentRepository.save(incident);

        List<User> initialRecipients = new ArrayList<>();
        initialRecipients.add(reporter);
        initialRecipients.addAll(administratorRepository.findAll());

        eventPublisher.publish(new IncidentDomainEvent(
            saved,
            reporter.getUsername(),
            reporter.getRole().name(),
            "INCIDENT_CREATED",
            "NEW",
            saved.getStatus().name(),
            "Incident " + saved.getIncidentKey() + " reported for service '" + service.getName() +
            "' with calculated priority " + saved.getPriority().getLabel(),
            priorityResult.getReasoning(),
            NotificationType.INCIDENT_CREATED,
            initialRecipients
        ));

        duplicateDetectionService.detectAndRecordSimilarIncidents(saved);

        Optional<AssignmentRecommendation> recommendation = recommendTechnicianForIncident(saved);
        if (recommendation.isPresent()) {
            AssignmentRecommendation rec = recommendation.get();
            saved.setRecommendedTechnicianReason(rec.getReason());
            assignIncidentToTechnician(
                saved.getId(),
                rec.getTechnician().getId(),
                "SYSTEM_AUTO_ASSIGN",
                rec.getStrategyName(),
                rec.getReason()
            );
        } else {
            saved.setRecommendedTechnicianReason("No available technician within capacity limits — queued for manual assignment.");
            incidentRepository.save(saved);
        }

        return getIncidentById(saved.getId());
    }

    @Transactional(readOnly = true)
    public Optional<AssignmentRecommendation> recommendTechnicianForIncident(Incident incident) {
        List<Technician> availableTechs = technicianRepository.findByAvailableTrueAndActiveTrue();
        return getActiveAssignmentStrategy().recommendTechnician(incident, availableTechs);
    }

    public Incident assignIncidentToTechnician(Long incidentId,
                                               Long technicianId,
                                               String actorUsername,
                                               String strategyName,
                                               String reason) {
        Incident incident = getIncidentById(incidentId);
        Technician newTech = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new UserNotFoundException("Technician not found: " + technicianId));

        if (!newTech.isAvailable() || !newTech.isActive()) {
            throw new TechnicianUnavailableException(
                "Technician " + newTech.getFullName() + " is currently marked unavailable and cannot be assigned."
            );
        }
        if (newTech.getCurrentWorkload() >= newTech.getMaxWorkload()) {
            throw new TechnicianWorkloadExceededException(
                "Technician " + newTech.getFullName() + " has reached maximum active workload (" +
                newTech.getCurrentWorkload() + "/" + newTech.getMaxWorkload() + ")."
            );
        }

        Technician previousTech = incident.getAssignedTechnician();
        if (previousTech != null && !previousTech.getId().equals(newTech.getId())) {
            previousTech.decrementWorkload();
            technicianRepository.save(previousTech);
        }

        if (previousTech == null || !previousTech.getId().equals(newTech.getId())) {
            newTech.incrementWorkload();
            technicianRepository.save(newTech);
        }

        String prevStatus = incident.getStatus().name();
        incident.setAssignedTechnician(newTech);
        incident.setAssignedAt(LocalDateTime.now());
        if (reason != null && !reason.isBlank()) {
            incident.setRecommendedTechnicianReason(reason);
        }

        if (incident.getStatus() == IncidentStatus.NEW ||
            incident.getStatus() == IncidentStatus.CLASSIFIED ||
            incident.getStatus() == IncidentStatus.REOPENED ||
            incident.getStatus() == IncidentStatus.ESCALATED) {
            stateMachine.transition(incident, IncidentStatus.ASSIGNED);
        }

        Incident saved = incidentRepository.save(incident);

        Assignment assignment = new Assignment(
            saved,
            newTech,
            actorUsername,
            strategyName != null ? strategyName : "MANUAL_OVERRIDE",
            reason != null ? reason : "Assigned to " + newTech.getFullName()
        );
        assignmentRepository.save(assignment);

        eventPublisher.publish(new IncidentDomainEvent(
            saved,
            actorUsername,
            "SYSTEM",
            "INCIDENT_ASSIGNED",
            prevStatus,
            saved.getStatus().name(),
            "Incident " + saved.getIncidentKey() + " assigned to Technician " + newTech.getFullName() +
            " (" + newTech.getTechnicianCode() + ")",
            reason,
            NotificationType.INCIDENT_ASSIGNED,
            List.of(newTech, saved.getReporter())
        ));

        return saved;
    }

    public Incident acknowledgeIncident(Long incidentId, User actor) {
        Incident incident = getIncidentById(incidentId);
        verifyTechnicianOrAdminPermission(incident, actor);

        String prevStatus = incident.getStatus().name();
        stateMachine.transition(incident, IncidentStatus.ACKNOWLEDGED);
        incident.setSlaRiskLevel(slaRiskDetectionService.evaluateRiskLevel(incident, LocalDateTime.now()));
        Incident saved = incidentRepository.save(incident);

        eventPublisher.publish(new IncidentDomainEvent(
            saved,
            actor.getUsername(),
            actor.getRole().name(),
            "INCIDENT_ACKNOWLEDGED",
            prevStatus,
            saved.getStatus().name(),
            "Technician " + actor.getFullName() + " acknowledged incident " + saved.getIncidentKey(),
            "First response timestamp recorded",
            NotificationType.INCIDENT_ACKNOWLEDGED,
            List.of(saved.getReporter())
        ));

        return saved;
    }

    public Incident startInvestigation(Long incidentId, User actor) {
        Incident incident = getIncidentById(incidentId);
        verifyTechnicianOrAdminPermission(incident, actor);

        String prevStatus = incident.getStatus().name();
        if (incident.getStatus() == IncidentStatus.ASSIGNED) {
            stateMachine.transition(incident, IncidentStatus.ACKNOWLEDGED);
        }
        stateMachine.transition(incident, IncidentStatus.IN_PROGRESS);
        Incident saved = incidentRepository.save(incident);

        eventPublisher.publish(new IncidentDomainEvent(
            saved,
            actor.getUsername(),
            actor.getRole().name(),
            "INVESTIGATION_STARTED",
            prevStatus,
            saved.getStatus().name(),
            "Active technical investigation started on " + saved.getIncidentKey() + " by " + actor.getFullName(),
            null,
            null,
            List.of()
        ));
        return saved;
    }

    public Incident requestReassignment(Long incidentId, String reason, Technician technician) {
        Incident incident = getIncidentById(incidentId);
        verifyTechnicianOrAdminPermission(incident, technician);

        String prevStatus = incident.getStatus().name();
        technician = incident.getAssignedTechnician();
        technician.decrementWorkload();
        technicianRepository.save(technician);

        incident.setAssignedTechnician(null);
        stateMachine.transition(incident, IncidentStatus.CLASSIFIED);
        Incident saved = incidentRepository.save(incident);

        InvestigationNote note = new InvestigationNote(
            saved, technician, "Reassignment requested by technician: " + reason, true
        );
        noteRepository.save(note);

        List<User> admins = new ArrayList<>(administratorRepository.findAll());
        eventPublisher.publish(new IncidentDomainEvent(
            saved,
            technician.getUsername(),
            technician.getRole().name(),
            "REASSIGNMENT_REQUESTED",
            prevStatus,
            saved.getStatus().name(),
            "Technician " + technician.getFullName() + " requested reassignment for " + saved.getIncidentKey() + ": " + reason,
            reason,
            NotificationType.INCIDENT_ASSIGNED,
            admins
        ));

        return saved;
    }

    public InvestigationNote addInvestigationNote(Long incidentId, String content, boolean internalOnly, User author) {
        Incident incident = getIncidentById(incidentId);
        verifyViewPermission(incident, author);

        if (content == null || content.isBlank()) {
            throw new InvalidIncidentException("Investigation note content cannot be empty.");
        }

        InvestigationNote note = new InvestigationNote(incident, author, content.trim(), internalOnly);
        InvestigationNote savedNote = noteRepository.save(note);

        if (author.getRole() == Role.ROLE_TECHNICIAN && incident.getFirstResponseAt() == null) {
            incident.setFirstResponseAt(LocalDateTime.now());
        }
        incident.setUpdatedAt(LocalDateTime.now());
        incidentRepository.save(incident);

        eventPublisher.publish(new IncidentDomainEvent(
            incident,
            author.getUsername(),
            author.getRole().name(),
            internalOnly ? "INTERNAL_NOTE_ADDED" : "PUBLIC_UPDATE_ADDED",
            incident.getStatus().name(),
            incident.getStatus().name(),
            author.getFullName() + " added an update on " + incident.getIncidentKey(),
            internalOnly ? "Internal technical note" : "Visible to reporter",
            null,
            List.of()
        ));

        return savedNote;
    }

    public InformationRequest requestAdditionalInformation(Long incidentId, String requestMessage, Technician technician) {
        Incident incident = getIncidentById(incidentId);
        verifyTechnicianOrAdminPermission(incident, technician);

        String prevStatus = incident.getStatus().name();
        if (incident.getStatus() == IncidentStatus.ASSIGNED || incident.getStatus() == IncidentStatus.ACKNOWLEDGED) {
            stateMachine.transition(incident, IncidentStatus.IN_PROGRESS);
        }
        stateMachine.transition(incident, IncidentStatus.WAITING_FOR_USER);
        incidentRepository.save(incident);

        InformationRequest req = new InformationRequest(incident, technician, requestMessage.trim());
        InformationRequest savedReq = infoRequestRepository.save(req);

        eventPublisher.publish(new IncidentDomainEvent(
            incident,
            technician.getUsername(),
            technician.getRole().name(),
            "INFO_REQUESTED",
            prevStatus,
            incident.getStatus().name(),
            "Technician " + technician.getFullName() + " requested additional information on " + incident.getIncidentKey(),
            requestMessage,
            NotificationType.INFO_REQUESTED,
            List.of(incident.getReporter())
        ));

        return savedReq;
    }

    public Incident respondToInformationRequest(Long incidentId, String responseText, Employee reporter) {
        Incident incident = getIncidentById(incidentId);
        if (!incident.getReporter().getId().equals(reporter.getId())) {
            throw new UnauthorizedOperationException("Only the reporting employee can respond to this incident's information request.");
        }

        List<InformationRequest> requests = infoRequestRepository.findByIncidentOrderByRequestedAtDesc(incident);
        for (InformationRequest req : requests) {
            if (!req.isResponded()) {
                req.recordResponse(responseText.trim());
                infoRequestRepository.save(req);
                break;
            }
        }

        InvestigationNote note = new InvestigationNote(
            incident, reporter, "Reporter provided requested information: " + responseText.trim(), false
        );
        noteRepository.save(note);

        String prevStatus = incident.getStatus().name();
        if (incident.getStatus() == IncidentStatus.WAITING_FOR_USER) {
            stateMachine.transition(incident, IncidentStatus.IN_PROGRESS);
        }
        Incident saved = incidentRepository.save(incident);

        List<User> recipients = new ArrayList<>();
        if (saved.getAssignedTechnician() != null) {
            recipients.add(saved.getAssignedTechnician());
        }

        eventPublisher.publish(new IncidentDomainEvent(
            saved,
            reporter.getUsername(),
            reporter.getRole().name(),
            "USER_INFO_PROVIDED",
            prevStatus,
            saved.getStatus().name(),
            "Employee " + reporter.getFullName() + " provided additional information for " + saved.getIncidentKey(),
            responseText,
            NotificationType.INFO_REQUESTED,
            recipients
        ));

        return saved;
    }

    public Incident resolveIncident(Long incidentId, ResolutionRequest request, User actor) {
        if (request == null || request.getRootCause() == null || request.getRootCause().isBlank() ||
                request.getResolutionSummary() == null || request.getResolutionSummary().isBlank() || request.getTimeSpentMinutes() < 1) {
            throw new InvalidIncidentException("Enter a root cause, resolution and time spent.");
        }
        Incident incident = getIncidentById(incidentId);
        verifyTechnicianOrAdminPermission(incident, actor);

        Technician resolver = incident.getAssignedTechnician();
        if (resolver == null) {
            throw new InvalidIncidentException("Incident must have an assigned technician before marking resolved.");
        }

        String prevStatus = incident.getStatus().name();
        if (incident.getStatus() == IncidentStatus.ASSIGNED) {
            stateMachine.transition(incident, IncidentStatus.ACKNOWLEDGED);
        }
        if (incident.getStatus() == IncidentStatus.ACKNOWLEDGED || incident.getStatus() == IncidentStatus.WAITING_FOR_USER) {
            stateMachine.transition(incident, IncidentStatus.IN_PROGRESS);
        }
        stateMachine.transition(incident, IncidentStatus.RESOLVED);

        LocalDateTime now = LocalDateTime.now();
        incident.setResolvedAt(now);
        if (incident.getResolutionDeadline() != null && now.isAfter(incident.getResolutionDeadline())) {
            incident.setResolutionSlaViolated(true);
            incident.setSlaRiskLevel(SLARiskLevel.BREACHED);
        } else {
            incident.setSlaRiskLevel(SLARiskLevel.LOW);
        }

        Resolution resolution = resolutionRepository.findByIncident(incident)
                .orElseGet(Resolution::new);
        resolution.setIncident(incident);
        resolution.setResolvedBy(resolver);
        resolution.setRootCause(request.getRootCause().trim());
        resolution.setResolutionSummary(request.getResolutionSummary().trim());
        resolution.setWorkaroundApplied(request.getWorkaroundApplied());
        resolution.setTimeSpentMinutes(Math.max(1, request.getTimeSpentMinutes()));
        resolution.setResolvedAt(now);
        resolutionRepository.save(resolution);
        incident.setResolution(resolution);

        resolver.decrementWorkload();
        technicianRepository.save(resolver);

        Incident saved = incidentRepository.save(incident);

        eventPublisher.publish(new IncidentDomainEvent(
            saved,
            actor.getUsername(),
            actor.getRole().name(),
            "INCIDENT_RESOLVED",
            prevStatus,
            saved.getStatus().name(),
            "Incident " + saved.getIncidentKey() + " resolved by " + resolver.getFullName() +
            ": " + request.getResolutionSummary(),
            "Root Cause: " + request.getRootCause(),
            NotificationType.INCIDENT_RESOLVED,
            List.of(saved.getReporter())
        ));

        return saved;
    }

    public Incident verifyAndCloseIncident(Long incidentId, FeedbackRequest feedbackRequest, User actor) {
        if (feedbackRequest == null || feedbackRequest.getRating() < 1 || feedbackRequest.getRating() > 5) {
            throw new InvalidIncidentException("Choose a rating from 1 to 5.");
        }
        Incident incident = getIncidentById(incidentId);
        if (actor == null || actor.getRole() != Role.ROLE_ADMIN && (actor.getRole() != Role.ROLE_EMPLOYEE ||
                !incident.getReporter().getId().equals(actor.getId()))) {
            throw new UnauthorizedOperationException("You can only verify and close incidents that you reported.");
        }
        if (incident.getStatus() != IncidentStatus.RESOLVED && incident.getStatus() != IncidentStatus.VERIFIED) {
            throw new InvalidStateTransitionException(
                "Incident " + incident.getIncidentKey() + " cannot be verified/closed while in status " + incident.getStatus()
            );
        }

        String prevStatus = incident.getStatus().name();
        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            stateMachine.transition(incident, IncidentStatus.VERIFIED);
            eventPublisher.publish(new IncidentDomainEvent(
                incident,
                actor.getUsername(),
                actor.getRole().name(),
                "INCIDENT_VERIFIED",
                prevStatus,
                IncidentStatus.VERIFIED.name(),
                "Resolution for " + incident.getIncidentKey() + " verified by " + actor.getFullName(),
                null,
                NotificationType.INCIDENT_VERIFIED,
                incident.getAssignedTechnician() != null ? List.of(incident.getAssignedTechnician()) : List.of()
            ));
        }

        stateMachine.transition(incident, IncidentStatus.CLOSED);
        incident.setClosureNotes(feedbackRequest.getClosureNotes() != null && !feedbackRequest.getClosureNotes().isBlank()
                ? feedbackRequest.getClosureNotes().trim()
                : "Verified and closed after successful resolution confirmation.");

        if (feedbackRepository.findByIncident(incident).isEmpty()) {
            IncidentFeedback feedback = new IncidentFeedback(
                incident,
                incident.getReporter(),
                feedbackRequest.getRating(),
                feedbackRequest.getComments()
            );
            feedbackRepository.save(feedback);
            incident.setFeedback(feedback);
        }

        Incident saved = incidentRepository.save(incident);

        eventPublisher.publish(new IncidentDomainEvent(
            saved,
            actor.getUsername(),
            actor.getRole().name(),
            "INCIDENT_CLOSED",
            IncidentStatus.VERIFIED.name(),
            saved.getStatus().name(),
            "Incident " + saved.getIncidentKey() + " closed with rating " + feedbackRequest.getRating() + "/5 stars.",
            feedbackRequest.getComments(),
            NotificationType.INCIDENT_CLOSED,
            saved.getAssignedTechnician() != null ? List.of(saved.getAssignedTechnician()) : List.of()
        ));

        return saved;
    }

    public Incident recalculateAndUpdatePriority(Long incidentId, ImpactLevel newImpact, UrgencyLevel newUrgency,
                                                 int affectedUsers, User actor) {
        Incident incident = getIncidentById(incidentId);
        PriorityLevel oldPriority = incident.getPriority();

        incident.setImpact(newImpact);
        incident.setUrgency(newUrgency);
        incident.setAffectedUsers(Math.max(1, affectedUsers));

        PriorityResult result = getActivePriorityStrategy().calculatePriority(
            newImpact, newUrgency, incident.getService().getBusinessCriticality(),
            affectedUsers, incident.getCategory(), incident.getService()
        );

        incident.setPriority(result.getPriorityLevel());
        incident.setPriorityScore(result.getCalculatedScore());
        incident.setPriorityReasoning(result.getReasoning());

        SLAPolicy newPolicy = catalogService.getSLAPolicyForPriority(result.getPriorityLevel());
        incident.setSlaPolicy(newPolicy);
        incident.setResponseDeadline(incident.getCreatedAt().plusMinutes(newPolicy.getResponseTargetMinutes()));
        incident.setResolutionDeadline(incident.getCreatedAt().plusMinutes(newPolicy.getResolutionTargetMinutes()));
        incident.setUpdatedAt(LocalDateTime.now());

        Incident saved = incidentRepository.save(incident);

        if (oldPriority != saved.getPriority()) {
            List<User> recipients = new ArrayList<>();
            recipients.add(saved.getReporter());
            if (saved.getAssignedTechnician() != null) {
                recipients.add(saved.getAssignedTechnician());
            }
            eventPublisher.publish(new IncidentDomainEvent(
                saved,
                actor.getUsername(),
                actor.getRole().name(),
                "PRIORITY_CHANGED",
                oldPriority.name(),
                saved.getPriority().name(),
                "Priority for " + saved.getIncidentKey() + " updated from " + oldPriority + " to " + saved.getPriority(),
                result.getReasoning(),
                NotificationType.PRIORITY_CHANGED,
                recipients
            ));
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public Incident getIncidentById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new InvalidIncidentException("Incident not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public Incident getIncidentByKey(String incidentKey) {
        return incidentRepository.findByIncidentKey(incidentKey)
                .orElseThrow(() -> new InvalidIncidentException("Incident not found with key: " + incidentKey));
    }

    @Transactional(readOnly = true)
    public Incident getIncidentAuthorized(Long id, User currentUser) {
        Incident incident = getIncidentById(id);
        verifyViewPermission(incident, currentUser);
        incident.getInvestigationNotes().size();
        incident.getInformationRequests().size();
        return incident;
    }

    public void verifyViewPermission(Incident incident, User currentUser) {
        if (currentUser == null || currentUser.getId() == null || currentUser.getRole() == null) {
            throw new UnauthorizedOperationException("Sign in to view an incident.");
        }
        if (currentUser.getRole() == Role.ROLE_TECHNICIAN) {
            verifyTechnicianOrAdminPermission(incident, currentUser);
        }
        if (currentUser.getRole() == Role.ROLE_EMPLOYEE &&
            (incident.getReporter() == null || !currentUser.getId().equals(incident.getReporter().getId()))) {
            throw new UnauthorizedOperationException(
                "Access Denied: Employees are strictly prohibited from viewing another employee's incident (" +
                incident.getIncidentKey() + ")."
            );
        }
    }

    public void verifyTechnicianOrAdminPermission(Incident incident, User currentUser) {
        if (currentUser.getRole() == Role.ROLE_ADMIN) {
            return;
        }
        if (currentUser.getRole() == Role.ROLE_TECHNICIAN) {
            if (incident.getAssignedTechnician() != null &&
                incident.getAssignedTechnician().getId().equals(currentUser.getId())) {
                return;
            }
            throw new UnauthorizedOperationException(
                "Technicians may only modify incidents currently assigned to them."
            );
        }
        throw new UnauthorizedOperationException("Insufficient permissions to perform technician action.");
    }

    @Transactional(readOnly = true)
    public Page<Incident> searchIncidents(IncidentSearchCriteria criteria, User currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100), Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Incident> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (currentUser.getRole() == Role.ROLE_EMPLOYEE) {
                predicates.add(cb.equal(root.get("reporter").get("id"), currentUser.getId()));
            }

            if (currentUser.getRole() == Role.ROLE_TECHNICIAN) {
                predicates.add(cb.equal(root.get("assignedTechnician").get("id"), currentUser.getId()));
            }
            if (criteria != null) {
                if (criteria.getQuery() != null && !criteria.getQuery().isBlank()) {
                    String pattern = "%" + criteria.getQuery().trim().toLowerCase() + "%";
                    Predicate keyMatch = cb.like(cb.lower(root.get("incidentKey")), pattern);
                    Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                    Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                    predicates.add(cb.or(keyMatch, titleMatch, descMatch));
                }
                if (criteria.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
                }
                if (criteria.getPriority() != null) {
                    predicates.add(cb.equal(root.get("priority"), criteria.getPriority()));
                }
                if (criteria.getCategoryId() != null) {
                    predicates.add(cb.equal(root.get("category").get("id"), criteria.getCategoryId()));
                }
                if (criteria.getServiceId() != null) {
                    predicates.add(cb.equal(root.get("service").get("id"), criteria.getServiceId()));
                }
                if (criteria.getTechnicianId() != null) {
                    predicates.add(cb.equal(root.get("assignedTechnician").get("id"), criteria.getTechnicianId()));
                }
                if (criteria.getSlaStatus() != null && !criteria.getSlaStatus().isBlank()) {
                    switch (criteria.getSlaStatus().toUpperCase()) {
                        case "VIOLATED" -> predicates.add(cb.or(
                            cb.isTrue(root.get("responseSlaViolated")),
                            cb.isTrue(root.get("resolutionSlaViolated"))
                        ));
                        case "COMPLIANT" -> predicates.add(cb.and(
                            cb.isFalse(root.get("responseSlaViolated")),
                            cb.isFalse(root.get("resolutionSlaViolated"))
                        ));
                        case "AT_RISK" -> predicates.add(root.get("slaRiskLevel").in(
                            SLARiskLevel.HIGH, SLARiskLevel.CRITICAL
                        ));
                    }
                }
                if (criteria.getFromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"), criteria.getFromDate().atStartOfDay()
                    ));
                }
                if (criteria.getToDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(
                        root.get("createdAt"), criteria.getToDate().plusDays(1).atStartOfDay()
                    ));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return incidentRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public List<Incident> getIncidentsForEmployee(Employee employee) {
        return incidentRepository.findByReporterOrderByCreatedAtDesc(employee);
    }

    @Transactional(readOnly = true)
    public List<Incident> getIncidentsForTechnician(Technician technician) {
        return incidentRepository.findByAssignedTechnicianOrderByPriorityAscCreatedAtDesc(technician);
    }

    @Transactional(readOnly = true)
    public List<Incident> getRecentIncidents() {
        return incidentRepository.findTop10ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}

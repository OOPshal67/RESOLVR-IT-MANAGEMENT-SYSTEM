package com.resolvr.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "information_requests")
public class InformationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "technician_id", nullable = false)
    private Technician technician;

    @Column(name = "request_message", nullable = false, columnDefinition = "TEXT")
    private String requestMessage;

    @Column(name = "employee_response", columnDefinition = "TEXT")
    private String employeeResponse;

    @Column(nullable = false)
    private boolean responded = false;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public InformationRequest() {
    }

    public InformationRequest(Incident incident, Technician technician, String requestMessage) {
        this.incident = incident;
        this.technician = technician;
        this.requestMessage = requestMessage;
        this.responded = false;
        this.requestedAt = LocalDateTime.now();
    }

    public void recordResponse(String responseText) {
        this.employeeResponse = responseText;
        this.responded = true;
        this.respondedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Incident getIncident() { return incident; }
    public void setIncident(Incident incident) { this.incident = incident; }
    public Technician getTechnician() { return technician; }
    public void setTechnician(Technician technician) { this.technician = technician; }
    public String getRequestMessage() { return requestMessage; }
    public void setRequestMessage(String requestMessage) { this.requestMessage = requestMessage; }
    public String getEmployeeResponse() { return employeeResponse; }
    public void setEmployeeResponse(String employeeResponse) { this.employeeResponse = employeeResponse; }
    public boolean isResponded() { return responded; }
    public void setResponded(boolean responded) { this.responded = responded; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
}

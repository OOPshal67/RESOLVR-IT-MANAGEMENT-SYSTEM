package com.resolvr.entity;

import com.resolvr.enums.RelationshipType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident_relationships")
public class IncidentRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_incident_id", nullable = false)
    private Incident sourceIncident;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "target_incident_id", nullable = false)
    private Incident targetIncident;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 30)
    private RelationshipType relationshipType;

    @Column(name = "similarity_score", nullable = false)
    private double similarityScore;

    @Column(nullable = false)
    private boolean confirmed = false;

    @Column(name = "detected_by", nullable = false, length = 60)
    private String detectedBy = "SYSTEM";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public IncidentRelationship() {
    }

    public IncidentRelationship(Incident sourceIncident, Incident targetIncident,
                                RelationshipType relationshipType, double similarityScore, String detectedBy) {
        this.sourceIncident = sourceIncident;
        this.targetIncident = targetIncident;
        this.relationshipType = relationshipType;
        this.similarityScore = similarityScore;
        this.confirmed = false;
        this.detectedBy = detectedBy;
        this.createdAt = LocalDateTime.now();
    }

    public int getSimilarityPercentage() {
        return (int) Math.round(similarityScore * 100.0);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Incident getSourceIncident() { return sourceIncident; }
    public void setSourceIncident(Incident sourceIncident) { this.sourceIncident = sourceIncident; }
    public Incident getTargetIncident() { return targetIncident; }
    public void setTargetIncident(Incident targetIncident) { this.targetIncident = targetIncident; }
    public RelationshipType getRelationshipType() { return relationshipType; }
    public void setRelationshipType(RelationshipType relationshipType) { this.relationshipType = relationshipType; }
    public double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(double similarityScore) { this.similarityScore = similarityScore; }
    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
    public String getDetectedBy() { return detectedBy; }
    public void setDetectedBy(String detectedBy) { this.detectedBy = detectedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

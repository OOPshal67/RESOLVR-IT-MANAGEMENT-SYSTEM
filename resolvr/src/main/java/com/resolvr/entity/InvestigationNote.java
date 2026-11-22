package com.resolvr.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "investigation_notes")
public class InvestigationNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "note_content", nullable = false, columnDefinition = "TEXT")
    private String noteContent;

    @Column(name = "internal_only", nullable = false)
    private boolean internalOnly = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public InvestigationNote() {
    }

    public InvestigationNote(Incident incident, User author, String noteContent, boolean internalOnly) {
        this.incident = incident;
        this.author = author;
        this.noteContent = noteContent;
        this.internalOnly = internalOnly;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Incident getIncident() { return incident; }
    public void setIncident(Incident incident) { this.incident = incident; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public String getNoteContent() { return noteContent; }
    public void setNoteContent(String noteContent) { this.noteContent = noteContent; }
    public boolean isInternalOnly() { return internalOnly; }
    public void setInternalOnly(boolean internalOnly) { this.internalOnly = internalOnly; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

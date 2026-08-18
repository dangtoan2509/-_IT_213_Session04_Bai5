package com.rikkei.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident_reports")
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false)
    private String orderCode;

    @Column(name = "license_plate", nullable = false)
    private String licensePlate;

    @Column(name = "incident_type", nullable = false)
    private String incidentType;

    @Column(name = "urgency", nullable = false)
    private String urgency;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status", nullable = false)
    private NotificationStatus notificationStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public IncidentReport() {
        this.createdAt = LocalDateTime.now();
    }

    public IncidentReport(String orderCode, String licensePlate, String incidentType, String urgency, String description, NotificationStatus notificationStatus) {
        this.orderCode = orderCode;
        this.licensePlate = licensePlate;
        this.incidentType = incidentType;
        this.urgency = urgency;
        this.description = description;
        this.notificationStatus = notificationStatus;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    public String getIncidentType() { return incidentType; }
    public void setIncidentType(String incidentType) { this.incidentType = incidentType; }
    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public NotificationStatus getNotificationStatus() { return notificationStatus; }
    public void setNotificationStatus(NotificationStatus notificationStatus) { this.notificationStatus = notificationStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

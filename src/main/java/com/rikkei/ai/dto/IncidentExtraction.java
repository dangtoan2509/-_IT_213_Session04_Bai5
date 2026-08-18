package com.rikkei.ai.dto;

public record IncidentExtraction(
    String orderCode,
    String licensePlate,
    String incidentType,
    String urgency,
    String description
) {}

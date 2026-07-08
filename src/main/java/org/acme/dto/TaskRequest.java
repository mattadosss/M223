package org.acme.dto;

import java.time.LocalDate;

public record TaskRequest(Long projectId, String title, String description, String status, LocalDate dueDate, Long assignedUserId) {
}

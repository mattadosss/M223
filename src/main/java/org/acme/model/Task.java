package org.acme.model;

import java.time.LocalDate;

public record Task(long id, long projectId, String title, String description, String status, LocalDate dueDate, Long assignedUserId) {
}

package org.acme.model;

import java.time.LocalDateTime;

public record Comment(long id, long taskId, long userId, String text, LocalDateTime createdAt) {
}

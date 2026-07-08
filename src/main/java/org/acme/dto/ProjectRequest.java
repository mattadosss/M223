package org.acme.dto;

import java.util.List;

public record ProjectRequest(String name, String description, Long ownerId, List<Long> memberIds) {
}

package org.acme.model;

import java.util.List;

public record Project(long id, String name, String description, long ownerId, List<Long> memberIds) {
}

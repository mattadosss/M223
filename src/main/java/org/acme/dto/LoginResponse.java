package org.acme.dto;

import org.acme.model.User;

public record LoginResponse(String message, String token, User user) {
}

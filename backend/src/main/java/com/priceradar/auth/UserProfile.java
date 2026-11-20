package com.priceradar.auth;

import java.time.Instant;

public record UserProfile(long id, String email, String fullName, String phone, Role role, Instant createdAt) {
}

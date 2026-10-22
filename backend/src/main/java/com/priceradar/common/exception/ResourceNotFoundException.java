package com.priceradar.common.exception;

/** Thrown when a requested entity does not exist. Maps to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}

	public static ResourceNotFoundException of(String resource, Object id) {
		return new ResourceNotFoundException("%s not found: %s".formatted(resource, id));
	}

}

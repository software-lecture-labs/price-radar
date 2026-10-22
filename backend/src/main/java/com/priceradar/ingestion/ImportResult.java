package com.priceradar.ingestion;

public record ImportResult(String path, Status status, int observations, int newProducts) {

	public enum Status {
		IMPORTED,
		ALREADY_IMPORTED,
		WOULD_IMPORT
	}

}

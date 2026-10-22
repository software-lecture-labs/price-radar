package com.priceradar.ingestion;

/** What importing one search file did, or would do in a dry run. */
public record ImportResult(String path, Status status, int observations, int newProducts) {

	public enum Status {
		/** Written in this run. */
		IMPORTED,
		/** The same bytes were imported before, at this or another path; nothing written. */
		ALREADY_IMPORTED,
		/** Checked against the database; nothing written. */
		WOULD_IMPORT
	}

}

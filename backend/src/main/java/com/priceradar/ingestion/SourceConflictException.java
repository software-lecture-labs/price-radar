package com.priceradar.ingestion;

/**
 * A file at an already imported path whose bytes have changed. Imported data is
 * history and is never overwritten, so this needs a person to look at it.
 */
public class SourceConflictException extends RuntimeException {

	public SourceConflictException(String path) {
		super(path + ": a different version of this file was already imported");
	}

}

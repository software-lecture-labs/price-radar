package com.priceradar.ingestion;

public class SourceConflictException extends RuntimeException {

	public SourceConflictException(String path) {
		super(path + ": a different version of this file was already imported");
	}

}

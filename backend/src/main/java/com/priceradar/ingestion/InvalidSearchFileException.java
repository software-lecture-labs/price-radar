package com.priceradar.ingestion;

public class InvalidSearchFileException extends RuntimeException {

	public InvalidSearchFileException(String message) {
		super(message);
	}

	public InvalidSearchFileException(String message, Throwable cause) {
		super(message, cause);
	}

}

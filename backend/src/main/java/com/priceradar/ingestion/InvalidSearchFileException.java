package com.priceradar.ingestion;

/** A saved search file that cannot be imported as it is. Nothing is written for it. */
public class InvalidSearchFileException extends RuntimeException {

	public InvalidSearchFileException(String message) {
		super(message);
	}

	public InvalidSearchFileException(String message, Throwable cause) {
		super(message, cause);
	}

}

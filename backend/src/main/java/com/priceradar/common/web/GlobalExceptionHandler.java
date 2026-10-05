package com.priceradar.common.web;

import java.net.URI;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.priceradar.common.exception.BusinessRuleException;
import com.priceradar.common.exception.ResourceNotFoundException;

import jakarta.validation.ConstraintViolationException;

/**
 * Translates exceptions into RFC 9457 {@code application/problem+json}.
 *
 * <p>Messages are written for API consumers; internal failures are logged with
 * their stack trace but answered with a generic body so nothing leaks.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final URI TYPE_VALIDATION = URI.create("https://priceradar.dev/problems/validation");
	private static final URI TYPE_NOT_FOUND = URI.create("https://priceradar.dev/problems/not-found");
	private static final URI TYPE_CONFLICT = URI.create("https://priceradar.dev/problems/conflict");
	private static final URI TYPE_INTERNAL = URI.create("https://priceradar.dev/problems/internal");

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail handleNotFound(ResourceNotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage(), TYPE_NOT_FOUND);
	}

	@ExceptionHandler(BusinessRuleException.class)
	ProblemDetail handleBusinessRule(BusinessRuleException ex) {
		return problem(HttpStatus.CONFLICT, "Request conflicts with current state", ex.getMessage(), TYPE_CONFLICT);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
		var detail = ex.getConstraintViolations().stream()
				.map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
				.sorted()
				.collect(Collectors.joining("; "));
		return problem(HttpStatus.BAD_REQUEST, "Validation failed", detail, TYPE_VALIDATION);
	}

	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail handleAuthentication(AuthenticationException ex) {
		return problem(HttpStatus.UNAUTHORIZED, "Authentication required", "Valid credentials are required.",
				TYPE_NOT_FOUND);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail handleAccessDenied(AccessDeniedException ex) {
		return problem(HttpStatus.FORBIDDEN, "Access denied", "You are not allowed to perform this action.",
				TYPE_NOT_FOUND);
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex, WebRequest request) {
		logger.error("Unhandled exception for " + request.getDescription(false), ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
				"The request could not be completed.", TYPE_INTERNAL);
	}

	/** Field-level validation errors, exposed as a {@code errors} extension. */
	@Override
	protected ProblemDetail createProblemDetail(Exception ex, org.springframework.http.HttpStatusCode status,
			String defaultDetail, String detailMessageCode, Object[] detailMessageArguments, WebRequest request) {

		var problemDetail = super.createProblemDetail(ex, status, defaultDetail, detailMessageCode,
				detailMessageArguments, request);

		if (ex instanceof MethodArgumentNotValidException validationException) {
			problemDetail.setType(TYPE_VALIDATION);
			problemDetail.setTitle("Validation failed");
			problemDetail.setProperty("errors", fieldErrors(validationException));
		}
		return problemDetail;
	}

	private static Map<String, String> fieldErrors(MethodArgumentNotValidException ex) {
		return ex.getBindingResult().getFieldErrors().stream()
				.sorted(Comparator.comparing(error -> error.getField()))
				.collect(Collectors.toMap(
						error -> error.getField(),
						error -> error.getDefaultMessage() == null ? "invalid" : error.getDefaultMessage(),
						(first, second) -> first + "; " + second,
						java.util.LinkedHashMap::new));
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail, URI type) {
		var problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
		problemDetail.setTitle(title);
		problemDetail.setType(type);
		return problemDetail;
	}

}

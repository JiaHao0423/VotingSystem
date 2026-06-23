package com.ben.com.backend.exception;

import java.time.Instant;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@NullMarked
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
		return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException ex) {
		return error(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
		return error(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
		return error(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
		return error(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.collect(java.util.stream.Collectors.toMap(
						FieldError::getField,
						error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "invalid",
						(first, second) -> first
				));
		return ResponseEntity.badRequest().body(Map.of(
				"timestamp", Instant.now().toString(),
				"status", HttpStatus.BAD_REQUEST.value(),
				"error", "Validation failed",
				"message", fieldErrors.values().stream().findFirst().orElse("資料驗證失敗"),
				"fields", fieldErrors
		));
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<Map<String, Object>> handleDataAccess(DataAccessException ex) {
		log.debug("Database access error", ex);
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "資料庫操作失敗，請稍後再試");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
		return error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage() != null ? ex.getMessage() : "伺服器發生錯誤");
	}

	private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(Map.of(
				"timestamp", Instant.now().toString(),
				"status", status.value(),
				"error", message
		));
	}
}

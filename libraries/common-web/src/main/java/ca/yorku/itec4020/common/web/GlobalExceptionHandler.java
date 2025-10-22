package ca.yorku.itec4020.common.web;

import ca.yorku.itec4020.common.dto.ApiError;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.RestController;

// Limit this JSON error handler to REST controllers only so MVC UI controllers can render HTML error pages
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
    String msg = ex.getBindingResult().getAllErrors().stream()
        .findFirst().map(e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "Validation error")
        .orElse("Validation error");
    return ResponseEntity.badRequest().body(new ApiError(msg));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex) {
    String msg = ex.getConstraintViolations().stream().findFirst()
        .map(v -> v.getMessage()).orElse("Constraint violation");
    return ResponseEntity.badRequest().body(new ApiError(msg));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneric(Exception ex) {
    return ResponseEntity.internalServerError().body(new ApiError("Unexpected error"));
  }
}

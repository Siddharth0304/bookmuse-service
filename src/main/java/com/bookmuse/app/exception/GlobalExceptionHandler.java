package com.bookmuse.app.exception;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
@RestControllerAdvice
@lombok.extern.slf4j.Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ProblemDetail api(ApiException ex) { return ProblemDetail.forStatusAndDetail(ex.status(),ex.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException ex) {
        var p=ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Request validation failed");
        p.setProperty("errors",ex.getBindingResult().getFieldErrors().stream()
            .map(e->e.getField()+": "+e.getDefaultMessage()).toList()); return p;
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        ConstraintViolationException.class, IllegalArgumentException.class})
    ProblemDetail badRequest(Exception ex) { return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Invalid request"); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflict(Exception ex) { return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"Resource conflicts with existing data"); }
    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception ex) {
        // Avoid exception messages: persistence failures can include personal request data.
        log.error("Unhandled API failure of type {}",ex.getClass().getName());
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,"Unexpected server error");
    }
}

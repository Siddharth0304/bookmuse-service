package com.bookmuse.app.exception;
import org.springframework.http.HttpStatus;
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    public ApiException(HttpStatus status,String message) { super(message); this.status=status; }
    public HttpStatus status() { return status; }
    public static ApiException missing() { return new ApiException(HttpStatus.NOT_FOUND,"Resource not found"); }
    public static ApiException forbidden() { return new ApiException(HttpStatus.FORBIDDEN,"Operation not allowed"); }
    public static ApiException conflict(String message) { return new ApiException(HttpStatus.CONFLICT,message); }
    public static ApiException unauthorized() { return new ApiException(HttpStatus.UNAUTHORIZED,"Invalid credentials or token"); }
}

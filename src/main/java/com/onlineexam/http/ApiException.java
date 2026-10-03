package com.onlineexam.http;

/** Thrown by handlers to return a JSON error with a specific HTTP status. */
public class ApiException extends RuntimeException {
    public final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public static ApiException badRequest(String msg) { return new ApiException(400, msg); }
    public static ApiException unauthorized() { return new ApiException(401, "Please log in to continue"); }
    public static ApiException forbidden() { return new ApiException(403, "You do not have access to this resource"); }
    public static ApiException notFound(String what) { return new ApiException(404, what + " not found"); }
}

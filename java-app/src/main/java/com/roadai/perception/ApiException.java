package com.roadai.perception;

public class ApiException extends Exception {
  private final int statusCode;
  private final String errorCode;

  public ApiException(int statusCode, String errorCode, String message) {
    super(message);
    this.statusCode = statusCode;
    this.errorCode = errorCode;
  }

  public ApiException(String message, Throwable cause) {
    super(message, cause);
    this.statusCode = 0;
    this.errorCode = "UNKNOWN_ERROR";
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}

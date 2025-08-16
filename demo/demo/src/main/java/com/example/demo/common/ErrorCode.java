package com.ssafy.ottereview.common.exception;

public interface ErrorCode {
    
    String getCode();
    
    String getMessage();
    
    int getHttpStatus();
}

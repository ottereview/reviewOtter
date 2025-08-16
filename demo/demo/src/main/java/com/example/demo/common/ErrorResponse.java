package com.ssafy.ottereview.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private String errorCode;
    private String message;
    private LocalDateTime timestamp;
    private String path;
    
    public ErrorResponse(ErrorCode errorCode, String path) {
        this.errorCode = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.timestamp = LocalDateTime.now();
        this.path = path;
    }
}

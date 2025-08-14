package com.ssafy.ottereview.branch.exception;

import com.ssafy.ottereview.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BranchErrorCode implements ErrorCode {

    ;

    private final String code;
    private final String message;
    private final int httpStatus;
}

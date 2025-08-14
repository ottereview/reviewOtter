package com.ssafy.ottereview.branch.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BranchCreateRequest {
    private String name;
    private int minApproveCnt;
    private Long repoId;
}
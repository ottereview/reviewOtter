package com.ssafy.ottereview.branch.controller;

import com.ssafy.ottereview.branch.dto.BranchResponse;
import com.ssafy.ottereview.branch.dto.BranchRoleCreateRequest;
import com.ssafy.ottereview.branch.entity.Branch;
import com.ssafy.ottereview.branch.service.BranchService;
import com.ssafy.ottereview.branch.service.BranchServiceImpl;
import com.ssafy.ottereview.common.annotation.MvcController;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/repositories/{repo-id}/branches")
@MvcController
public class BranchController {

    private final BranchServiceImpl branchServiceImpl;

    @GetMapping()
    public ResponseEntity<?> getBranchList(@PathVariable(name ="repo-id") Long repoId){
        List<Branch> branches = branchServiceImpl.getBranchesByRepoId(repoId);
        List<BranchResponse> responseList = branches.stream()
                .map(branch -> BranchResponse.builder()
                        .id(branch.getId())
                        .name(branch.getName())
                        .minApproveCnt(branch.getMinApproveCnt())
                        .repo_id(branch.getRepo().getRepoId())
                        .build()
                )
                .toList();
        return ResponseEntity.ok(responseList);
    }

    @GetMapping("/{branch-id}")
    public ResponseEntity<?> getBranchByBranchId(@PathVariable(name = "branch-id") Long branchId){
        Branch branch = branchServiceImpl.getBranchById(branchId);
        BranchResponse branchResponse = BranchResponse.builder()
                .id(branch.getId())
                .name(branch.getName())
                .minApproveCnt(branch.getMinApproveCnt())
                .repo_id(branch.getRepo().getId())
                .build();
        return ResponseEntity.ok(branchResponse);
    }

    @PatchMapping("/roles")
    public ResponseEntity<?> updateBranchRole(@RequestBody BranchRoleCreateRequest branchRoleCreateRequest){
        return ResponseEntity.ok(branchServiceImpl.updateBranchRole(branchRoleCreateRequest));

    }
}

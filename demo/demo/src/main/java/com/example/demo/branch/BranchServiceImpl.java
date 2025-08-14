package com.ssafy.ottereview.branch.service;

import com.ssafy.ottereview.branch.dto.BranchCreateRequest;
import com.ssafy.ottereview.branch.dto.BranchRoleCreateRequest;
import com.ssafy.ottereview.branch.entity.Branch;
import com.ssafy.ottereview.branch.repository.BranchRepository;
import com.ssafy.ottereview.repo.entity.Repo;
import com.ssafy.ottereview.repo.repository.RepoRepository;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHBranchProtection;
import org.kohsuke.github.GHRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService{

    private final RepoRepository repoRepository;
    private final BranchRepository branchRepository;


    /**
     * branch_id값으로 branch를 가져온다.
     * @param id
     * @return branch 객체
     */
    @Override
    public Branch getBranchById(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Repo not found with ID " + id)
                );
    }

    @Override
    @Transactional
    public List<Branch> createBranchList(GHRepository ghRepository, Repo repo) {

        List<Branch> branchesToSave = new ArrayList<>();
        try {
            Map<String, GHBranch> branches = ghRepository.getBranches();

            for (Map.Entry<String, GHBranch> entry : branches.entrySet()) {
                GHBranch branch = entry.getValue();
                int requiredApprovals = 0;
                if (branch.isProtected()) {
                    GHBranchProtection protection = branch.getProtection();
                    GHBranchProtection.RequiredReviews requiredReviews = protection.getRequiredReviews();

                    if (requiredReviews != null) {
                        // 필요한 approve 개수 가져오기
                        requiredApprovals = requiredReviews.getRequiredReviewers();
                        System.out.println("Required approvals: " + requiredApprovals);
                    }
                }else {
                    System.out.println("Branch " + entry.getKey() + " is not protected. Skipping protection info.");
                }
                    Branch branch1 = Branch.builder()
                            .name(entry.getKey())
                            .minApproveCnt(requiredApprovals)
                            .repo(repo)  // 아직 저장되지 않은 repo 객체 참조
                            .build();

                    branchesToSave.add(branch1);

            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return branchesToSave;
    }

    /**
     * 사용자에게 branch 정보를 받거나 github로부터 동기화 해올때 db에 branch를 저장하는 코드
     * @param branchCreateRequest
     */
    public void createBranch(BranchCreateRequest branchCreateRequest) {
        // repo를 찾아오는 것을  Optional<Repo>로 해놔서 아래의 코드로 존재하는지 여부 확인후 builder 패턴에 넣어야한다.
        Repo repo = repoRepository.findById(branchCreateRequest.getRepoId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Repo not found with ID " + branchCreateRequest.getRepoId())
                );
        // branch table에 저장하기 위한 로직
        branchRepository.save(Branch.builder()
                .name(branchCreateRequest.getName())
                .minApproveCnt(branchCreateRequest.getMinApproveCnt())
                .repo(repo)
                .build());
    }

    /**
     * repoId를 가지고 branch list를 가져오는 코드
     * @param repoId
     * @return
     */
    @Override
    @Transactional
    public List<Branch> getBranchesByRepoId(Long repoId) {
        return branchRepository.findAllByRepo_Id(repoId);
    }

    @Override
    @Transactional
    public void saveAllBranchList(List<Branch> branches) {
        if(!branches.isEmpty()){
            branchRepository.saveAll(branches);
        }
    }

    @Override
    @Transactional
    public Branch updateBranchRole(BranchRoleCreateRequest branchRoleCreateRequest) {
        Branch branch = branchRepository.findById(branchRoleCreateRequest.getId()).orElseThrow();
        Branch newBranch = Branch.builder()
                .id(branch.getId())
                .minApproveCnt(branchRoleCreateRequest.getMinApproved())
                .name(branch.getName())
                .repo(branch.getRepo())
                .build();

        branchRepository.save(newBranch);
        log.info(branchRepository.getReferenceById(branchRoleCreateRequest.getId()).getMinApproveCnt()+"");
        return newBranch;
    }


}

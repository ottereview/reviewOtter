package com.ssafy.ottereview.branch.repository;

import com.ssafy.ottereview.branch.entity.Branch;
import com.ssafy.ottereview.repo.entity.Repo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    // repoId로 해당 레포의 브렌치 가져오기
    List<Branch> findAllByRepo_Id(Long repoId);

    // branch id로 branch 정보 가져오기
    Optional<Branch> findById(Long id);

    List<Branch> findAllByRepo(Repo repo);


    void deleteByNameAndRepo(String name, Repo repo);

    Branch findByNameAndRepo(String name, Repo repo);

    Boolean existsByNameAndRepo(String name, Repo repo);

    Boolean existsById(Long id);
}

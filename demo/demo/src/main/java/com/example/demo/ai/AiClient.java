package com.ssafy.ottereview.ai.client;

import com.ssafy.ottereview.account.service.UserAccountService;
import com.ssafy.ottereview.ai.dto.request.AiConventionRequest;
import com.ssafy.ottereview.ai.dto.request.AiRequest;
import com.ssafy.ottereview.ai.dto.response.AiConventionResponse;
import com.ssafy.ottereview.ai.dto.response.AiPriorityResponse;
import com.ssafy.ottereview.ai.dto.response.AiResult;
import com.ssafy.ottereview.ai.dto.response.AiReviewerResponse;
import com.ssafy.ottereview.ai.dto.response.AiSummaryResponse;
import com.ssafy.ottereview.ai.dto.response.AiTitleResponse;
import com.ssafy.ottereview.ai.repository.AiRedisRepository;
import com.ssafy.ottereview.merge.dto.MergedPullRequestInfo;
import com.ssafy.ottereview.user.entity.CustomUserDetail;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class AiClient {
    
    private final WebClient aiWebClient;
    private final UserAccountService userAccountService;
    private final AiRedisRepository aiRedisRepository;
    
    /**
     * PR 제목 생성
     */
    public Mono<AiTitleResponse> recommendTitle(AiRequest request) {
        
        return aiWebClient.post()
                .uri("/ai/pull_requests/title")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiTitleResponse.class)
                .timeout(Duration.ofMinutes(1))
                .doOnSuccess(title -> log.info("Title 생성 완료: {}", title))
                .doOnError(error -> log.error("Title 생성 실패", error))
                .onErrorReturn(createDefaultTitleResponse());  // 기본값 제공
    }
    
    /**
     * PR 요약 생성
     */
    public Mono<AiSummaryResponse> getSummary(AiRequest request) {
        
        return aiWebClient.post()
                .uri("/ai/pull_requests/summary")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiSummaryResponse.class)
                .timeout(Duration.ofMinutes(1))
                .doOnSuccess(summary -> log.info("Summary 생성 완료"))
                .doOnError(error -> log.error("Summary 생성 실패", error))
                .onErrorReturn(createDefaultSummaryResponse());
    }
    
    /**
     * 리뷰어 추천
     */
    public Mono<AiReviewerResponse> recommendReviewers(AiRequest request) {
        
        return aiWebClient.post()
                .uri("/ai/reviewers/recommend")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiReviewerResponse.class)
                .timeout(Duration.ofMinutes(1))
                .doOnSuccess(reviewers -> log.info("Reviewers 추천 완료: {}", reviewers))
                .doOnError(error -> log.error("Reviewers 추천 실패", error))
                .onErrorReturn(createDefaultReviewersResponse());
    }
    
    /**
     * 우선순위 추천
     */
    public Mono<AiPriorityResponse> recommendPriority(AiRequest request) {
        
        return aiWebClient.post()
                .uri("/ai/priority/recommend")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiPriorityResponse.class)
                .timeout(Duration.ofMinutes(1))
                .doOnSuccess(priority -> log.info("Priority 추천 완료: {}", priority))
                .doOnError(error -> log.error("Priority 추천 실패", error))
                .onErrorReturn(AiPriorityResponse.createDefaultPriorityResponse());
    }
    
    /**
     * 코딩 컨벤션 검사
     */
    public Mono<AiConventionResponse> checkCodingConvention(AiConventionRequest request) {
        
        return aiWebClient.post()
                .uri("/ai/coding-convention/check")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiConventionResponse.class)
                .timeout(Duration.ofMinutes(1))
                .doOnSuccess(conventions -> log.info("Coding Convention 검사 완료"))
                .doOnError(error -> log.error("Coding Convention 검사 실패", error))
                .onErrorReturn(createDefaultConventionResponse());
    }
    
    /**
     * 모든 AI 분석을 병렬로 실행
     */
    public Mono<AiResult> analyzeAll(CustomUserDetail customUserDetail, AiRequest request) {
        log.info("AI 전체 분석 시작");
        
        // 1. 캐시 조회를 Mono로 감싸서 reactive chain 유지
        return Mono.fromCallable(() -> aiRedisRepository.getAiInfo(request))
                .doOnNext(cachedResult -> {
                    if (cachedResult != null) {
                        log.info("캐시된 AI 정보 조회 성공 - 캐시 히트");
                    }
                })
                .filter(Objects::nonNull)  // null이 아닌 경우만 통과
                .switchIfEmpty(
                        // 2. 캐시가 없는 경우에만 실제 분석 수행
                        performFullAnalysis(customUserDetail, request)
                )
                .doOnSuccess(result -> log.info("AI 전체 분석 완료"))
                .doOnError(error -> log.error("AI 전체 분석 실패", error));
    }
    
    private Mono<AiResult> performFullAnalysis(CustomUserDetail customUserDetail, AiRequest request) {
        log.info("캐시 미스 - 새로운 AI 분석 시작");
        
        LocalDateTime startTime = LocalDateTime.now();
        
        // 3. 권한 검증을 비동기로 수행
        return validateUserPermissionAsync(customUserDetail.getUser()
                .getId(), request.getRepoId())
                .then(executeParallelAnalysis(request, startTime))
                .timeout(Duration.ofMinutes(5))  // 전체 타임아웃 5분
                .doOnSuccess(result -> {
                    Duration duration = Duration.between(startTime, LocalDateTime.now());
                    log.info("PR 전체 분석 완료 - 소요시간: {}초", duration.toSeconds());
                })
                .doOnError(error -> {
                    Duration duration = Duration.between(startTime, LocalDateTime.now());
                    log.error("PR 전체 분석 실패 - 소요시간: {}초", duration.toSeconds(), error);
                })
                .onErrorResume(error -> handlePartialFailure(startTime, error));
    }
    
    private Mono<AiResult> executeParallelAnalysis(AiRequest request, LocalDateTime startTime) {
        log.info("병렬 AI API 호출 시작");
        
        // 4. 각 API 호출에 개별 타임아웃과 fallback 추가
        Mono<AiTitleResponse> titleMono = recommendTitle(request)
                .timeout(Duration.ofMinutes(2))
                .doOnSubscribe(sub -> log.info("Title 분석 시작"))
                .doOnSuccess(result -> log.debug("Title 분석 완료"))
                .onErrorResume(error -> {
                    log.warn("Title 분석 실패, 기본값 사용", error);
                    return Mono.just(createDefaultTitleResponse());
                });
        
        Mono<AiReviewerResponse> reviewersMono = recommendReviewers(request)
                .timeout(Duration.ofMinutes(2))
                .doOnSubscribe(sub -> log.info("Reviewers 분석 시작"))
                .doOnSuccess(result -> log.debug("Reviewers 분석 완료"))
                .onErrorResume(error -> {
                    log.warn("Reviewers 분석 실패, 기본값 사용", error);
                    return Mono.just(createDefaultReviewersResponse());
                });
        
        Mono<AiPriorityResponse> priorityMono = recommendPriority(request)
                .timeout(Duration.ofMinutes(2))
                .doOnSubscribe(sub -> log.info("Priority 분석 시작"))
                .doOnSuccess(result -> log.debug("Priority 분석 완료"))
                .onErrorResume(error -> {
                    log.warn("Priority 분석 실패, 기본값 사용", error);
                    return Mono.just(createDefaultPriorityResponse());
                });
        
        // 5. 모든 결과를 조합하고 캐시 저장
        return Mono.zip(titleMono, reviewersMono, priorityMono)
                .map(results -> {
                    AiResult analysisResult = AiResult.builder()
                            .title(results.getT1())
                            .reviewers(results.getT2())
                            .priority(results.getT3())
                            .analysisTime(startTime)
                            .build();
                    
                    log.debug("AI 분석 결과 생성 완료");
                    return analysisResult;
                })
                // 6. 의미있는 값일 때만 캐시 저장을 비동기로 수행
                .flatMap(result -> {
                    if (isValidForCaching(result)) {
                        log.info("의미있는 AI 분석 결과 - 캐시에 저장합니다");
                        return saveToCache(request, result)
                                .thenReturn(result)
                                .onErrorResume(cacheError -> {
                                    log.warn("캐시 저장 실패, 결과는 정상 반환", cacheError);
                                    return Mono.just(result);
                                });
                    } else {
                        log.info("기본값이 포함된 AI 분석 결과 - 캐시에 저장하지 않습니다");
                        return Mono.just(result);
                    }
                });
    }
    
    
    
    public Mono<Void> saveVectorDb(MergedPullRequestInfo mergedPullRequestInfo) {
        log.debug("Vector DB 저장 시작 - PR ID: {}", mergedPullRequestInfo.getId());
        return aiWebClient.post()
                .uri("/ai/vector-db/store")
                .bodyValue(mergedPullRequestInfo)
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(Duration.ofMinutes(2))
                .doOnSuccess(result -> log.info("Vector DB 저장 완료 - PR ID: {}", mergedPullRequestInfo.getId()))
                .doOnError(error -> log.error("Vector DB 저장 실패 - PR ID: {}", mergedPullRequestInfo.getId(), error))
                .onErrorResume(error -> {
                    log.warn("Vector DB 저장 실패하였지만 계속 진행 - PR ID: {}", mergedPullRequestInfo.getId());
                    return Mono.empty();
                });
    }
    
    private Mono<Void> validateUserPermissionAsync(Long userId, Long repoId) {
        return Mono.fromCallable(() -> {
                    userAccountService.validateUserPermission(userId, repoId);
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
    
    // 8. 캐시 저장을 비동기로 수행
    private Mono<Void> saveToCache(AiRequest request, AiResult result) {
        return Mono.fromRunnable(() -> {
                    try {
                        aiRedisRepository.saveAiInfo(request, result);
                        log.debug("AI 분석 결과 캐시 저장 완료");
                    } catch (Exception e) {
                        log.warn("캐시 저장 중 오류 발생", e);
                        throw new RuntimeException("캐시 저장 실패", e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())  // I/O 스레드에서 실행
                .then();
    }
    
    
    // 10. 부분 실패 처리 개선
    private Mono<AiResult> handlePartialFailure(LocalDateTime startTime, Throwable error) {
        log.error("전체 분석 실패, 부분 결과 제공", error);
        
        return Mono.just(AiResult.builder()
                .title(createDefaultTitleResponse())
                .reviewers(createDefaultReviewersResponse())
                .priority(createDefaultPriorityResponse())
                .analysisTime(startTime)
                .hasErrors(true)  // 에러 플래그 추가
                .errorMessage(error.getMessage())
                .build());
    }
    
    /**
     * AI 분석 결과가 캐시에 저장할 가치가 있는지 검증
     * 모든 응답이 의미있는 값을 포함해야 함 (기본값/폴백값 제외)
     */
    private boolean isValidForCaching(AiResult result) {
        // null 체크
        if (result == null || result.getTitle() == null ||
            result.getReviewers() == null || result.getPriority() == null) {
            return false;
        }
        
        // Title 검증: 기본 에러 메시지가 아닌지 확인
        if (isDefaultTitleResponse(result.getTitle())) {
            log.debug("Title이 기본값입니다 - 캐시 저장 제외");
            return false;
        }
        
        // Reviewers 검증: 빈 리스트가 아닌지 확인
        if (isDefaultReviewersResponse(result.getReviewers())) {
            log.debug("Reviewers가 기본값입니다 - 캐시 저장 제외");
            return false;
        }
        
        // Priority 검증: 기본 우선순위가 아닌지 확인
        if (isDefaultPriorityResponse(result.getPriority())) {
            log.debug("Priority가 기본값입니다 - 캐시 저장 제외");
            return false;
        }
        
        return true;
    }
    
    /**
     * Title 응답이 기본값인지 검증
     */
    private boolean isDefaultTitleResponse(AiTitleResponse title) {
        return title.getResult() == null ||
               "분석 중 오류 발생".equals(title.getResult().trim());
    }
    
    /**
     * Reviewers 응답이 기본값인지 검증
     */
    private boolean isDefaultReviewersResponse(AiReviewerResponse reviewers) {
        return reviewers.getResult() == null || reviewers.getResult().isEmpty();
    }
    
    /**
     * Priority 응답이 기본값인지 검증
     */
    private boolean isDefaultPriorityResponse(AiPriorityResponse priority) {
        if (priority.getResult() == null || priority.getResult().getPriority() == null ||
            priority.getResult().getPriority().isEmpty()) {
            return true;
        }
        
        // 첫 번째 우선순위 항목이 기본값인지 확인
        AiPriorityResponse.PriorityItem firstItem = priority.getResult().getPriority().get(0);
        return "기본 우선순위".equals(firstItem.getTitle()) &&
               "우선순위를 수동으로 설정해주세요".equals(firstItem.getReason());
    }
}

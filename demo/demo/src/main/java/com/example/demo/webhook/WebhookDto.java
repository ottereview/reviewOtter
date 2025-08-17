package com.ssafy.ottereview.webhook.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.ottereview.common.annotation.MvcController;
import com.ssafy.ottereview.common.exception.BusinessException;
import com.ssafy.ottereview.user.entity.CustomUserDetail;
import com.ssafy.ottereview.user.entity.User;
import com.ssafy.ottereview.webhook.exception.WebhookErrorCode;
import com.ssafy.ottereview.webhook.service.BranchProtectionEventService;
import com.ssafy.ottereview.webhook.service.InstallationEventService;
import com.ssafy.ottereview.webhook.service.PullRequestEventService;
import com.ssafy.ottereview.webhook.service.PushEventService;
import com.ssafy.ottereview.webhook.service.RepoEventService;
import com.ssafy.ottereview.webhook.service.ReviewCommentEventService;
import com.ssafy.ottereview.webhook.service.ReviewEventService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Slf4j
@MvcController
public class GithubWebhookController {

    private final PushEventService pushEventService;
    private final InstallationEventService installationEventService;
    private final PullRequestEventService pullRequestEventService;
    private final ReviewEventService reviewEventService;
    private final ReviewCommentEventService reviewCommentEventService;
    private final BranchProtectionEventService branchProtectionEventService;
    private final RepoEventService repoEventService;
    private final ObjectMapper objectMapper;

    @Hidden
    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-GitHub-Event") String event,
            @RequestHeader("X-GitHub-Delivery") String delivery,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            String action = jsonNode.path("action").asText();
            log.debug("[웹훅 이벤트 수신] 이벤트: {}, Action: {}", event, action);
            
        } catch (Exception e) {
            log.error("Error parsing payload: {}", e.getMessage());
        }
        // 이벤트별 처리
        switch (event) {
            case "push":
                pushEventService.processPushEvent(payload);
                break;

            case "pull_request":
                log.info("Handling pull request event");
                pullRequestEventService.processPullRequestEvent(payload);
                break;

            case "pull_request_review":
                log.info("Handling pull request review event");
                reviewEventService.processReviewEvent(payload);
                // pull_request_review 이벤트는 리뷰 코멘트와 다름
                // 별도의 처리 로직이 필요할 수 있음
                log.debug(signature != null ? "Signature: " + signature : "No signature provided");
                break;

            case "pull_request_review_comment":
                log.info("Handling pull request review event");
                reviewCommentEventService.processReviewCommentEvent(payload);
                                // pull_request_review 이벤트는 리뷰 코멘트와 다름
                // 별도의 처리 로직이 필요할 수 있음
                log.debug(signature != null ? "Signature: " + signature : "No signature provided");
                break;

            case "installation":
                log.info("Handling installation event");
                installationEventService.processInstallationEvent(payload);
                break;

            case "installation_repositories":
                log.info("Handling installation repositories event");
                installationEventService.processInstallationRepositoriesEvent(payload);
                break;

            case "create":
                log.info("Handling installation create Branch event");
                installationEventService.processAddBranchesEvent(payload);
                break;

            case "delete":
                log.info("Handling installation delete Branch event");
                installationEventService.processDeleteBranchesEvent(payload);
                break;

            case "branch_protection_rule":
                log.info("Handling branch protection rule event");
                og.deubg("Hi");
                og.deubg("Hi");
                og.deubg("Hi");
                branchProtectionEventService.processBranchProtection(payload);
                break;  log.deubg("H
                  log.deubg("H
                    log.deubg("H
                      log.deubg("H
     log.deubg("Hi");   
                      log.deubg("Hi");
             log.deubg("Hi");
                log.deubg("Hi");               log.info("Handling repository event");
          log.deubg("Hi");     log.deubg("Hi");   
                      log.deubg("Hi");
             log.deubg("Hi");
                log.deubg("Hi");               log.info("Handling repository event");
          log.deubg("Hi");
            case "repository":
                         log.deubg("Hi");
                log.deubg("Hi");             log.deubg("Hi");
                log.deubg("Hi");
             log.deubg("Hi");
                log.deubg("Hi");   
                      log.deubg("Hi");
             log.deubg("Hi");
                log.deubg("Hi");               log.info("Handling repository event");
          log.deubg("Hi");
                log.deubg("Hi");
                repoEventService.processRepo(payload);
                break;

                log.deubg("Hi");
            default:
                throw new BusinessException(WebhookErrorCode.WEBHOOK_UNSUPPORTED_EVENT);
        }

        return ResponseEntity.ok("OK");
    }
}

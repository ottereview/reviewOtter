package com.ssafy.ottereview.auth.service;

import com.ssafy.ottereview.auth.dto.GithubUserDto;
import com.ssafy.ottereview.auth.exception.AuthErrorCode;
import com.ssafy.ottereview.auth.jwt.dto.LoginResponseDto;
import com.ssafy.ottereview.auth.jwt.service.TokenService;
import com.ssafy.ottereview.auth.jwt.util.JwtUtil;
import com.ssafy.ottereview.common.exception.BusinessException;
import com.ssafy.ottereview.user.entity.User;
import com.ssafy.ottereview.user.exception.UserErrorCode;
import com.ssafy.ottereview.user.repository.UserRepository;
import com.ssafy.ottereview.user.service.UserService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Value("${github.oauth.client-id}")
    private String clientId;
    @Value("${github.oauth.client-secret}")
    private String clientSecret;
    @Value("${github.oauth.redirect-uri}")
    private String redirectUri;

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final RestTemplate restTemplate;

    @Override
    @Transactional
    public LoginResponseDto githubLogin(String code) {
        // GitHub Access Token 요청
        String githubAccessToken = requestGithubAccessToken(code);
        // GitHub 사용자 정보 요청
        GithubUserDto githubUser = requestGithubUser(githubAccessToken);
        // DB 사용자 조회 or 회원가입
        User user = userRepository.findByGithubId(githubUser.getId())
                .orElseGet(() -> registerUser(githubUser));
        // JWT 발급 및 Refresh Token 저장
        String accessToken = jwtUtil.createAccessToken(user);
        String refreshToken = jwtUtil.createRefreshToken(user);
        tokenService.saveRefreshToken(user.getId(), refreshToken);
        return new LoginResponseDto(accessToken, refreshToken);
    }

    private User registerUser(GithubUserDto githubUser) {
        User user = User.builder()
                .githubUsername(githubUser.getLogin())
                .githubId(githubUser.getId())
                .githubEmail(githubUser.getEmail())
                .type(githubUser.getType())
                .profileImageUrl(githubUser.getAvatarUrl())
                .rewardPoints(0)
                .userGrade("BASIC")
                .build();

        userService.createUser(user);
        return userRepository.save(user);
    }

    public GithubUserDto requestGithubUser(String githubAccessToken) {
        try {
            // 토큰으로 GitHub 클라이언트 생성
            GitHub github = new GitHubBuilder()
                    .withOAuthToken(githubAccessToken)
                    .build();

            GHMyself myself = github.getMyself();

            String login = myself.getLogin();
            String email = myself.getEmail();
            Long githubId = myself.getId();
            String type = myself.getType();

            // 이메일이 없거나 private일 경우, 추가로 primary 이메일을 찾아야 함
            if (email == null || email.isEmpty()) {
                email = github.getMyself().listEmails().toList().stream()
                        .filter(e -> e.isPrimary() && e.isVerified())
                        .map(e -> e.getEmail())
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(AuthErrorCode.GITHUB_USER_NOT_FOUND));
            }

            String avatarUrl = myself.getAvatarUrl();

            return new GithubUserDto(login, githubId, email, type, avatarUrl);

        } catch (Exception e) {
            throw new BusinessException(AuthErrorCode.GITHUB_USER_API_ERROR);
        }
    }


    private String requestGithubAccessToken(String code) {
        log.info("clientId={}, clientSecret={}", clientId, clientSecret);
        String url = "https://github.com/login/oauth/access_token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
//        params.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map<String, Object>> responseEntity =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        request,
                        new ParameterizedTypeReference<>() {
                        }
                );

        Map<String, Object> response = responseEntity.getBody();
        return (String) response.get("access_token");
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDto refreshAccessToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        Long userId = Long.valueOf(jwtUtil.getClaims(refreshToken).getSubject());

        String storedToken = tokenService.getRefreshToken(userId);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        String newAccessToken = jwtUtil.createAccessToken(user);
        return new LoginResponseDto(newAccessToken, refreshToken);
    }

    public void logout(Long userId) {
        tokenService.deleteRefreshToken(userId);
    }
}

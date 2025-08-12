@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    PR_NOT_FOUND("PR001", "PullRequest를 찾을 수 없습니다", 404),
    PR_ALREADY_EXISTS("PR002", "이미 존재하는 PullRequest입니다", 409),
    PR_NOT_AUTHORIZED("PR003", "PullRequest에 대한 권한이 없습니다", 403)

    private final String code;
    private final String message;
    private final int httpStatus;
}

package com.example.demo.email;

public class EmailService {
       private final EmailUtil emailUtil;

    @Value("${app.front.url}")
    private String frontUrl;

    /**
     * 채팅방 초대 메일 발송
     */
    @Async
    public void sendChatInvite(EmailRequestDto.ChatInvite invite) {
        try {
            // 초대 링크 생성
            String link = frontUrl + "/chatroom/" + invite.getRoomId();

            // 메일 DTO 생성
            EmailResponseDto.EmailMessage emailMessage = EmailResponseDto.EmailMessage.builder()
                    .to(invite.getEmail()).subject("[ottereview] 채팅방 초대: " + invite.getRoomName())
                    .roomName(invite.getRoomName()).inviterName(invite.getInviterName())
                    .roomLink(link).build();

            // 메일 발송
            emailUtil.sendChatRoomInviteMail(emailMessage, "email/chat-invite");

        } catch (Exception e) {
            // 비동기 메서드 예외는 호출자에게 전달되지 않으므로 로그 필수
            log.error("채팅방 초대 메일 발송 실패 - 대상: {}, 에러: {}", invite.getEmail(), e.getMessage(), e);
        }
    }
}

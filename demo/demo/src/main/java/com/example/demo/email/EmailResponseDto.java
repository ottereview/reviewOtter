package com.ssafy.ottereview.email.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class EmailResponseDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmailMessage {
        private String to;        // 수신자 이메일
        private String subject;   // 메일 제목
        private String message;   // 템플릿에 들어갈 메시지

        // 초대 메일에 필요한 경우
        private String roomName;      // 채팅방 이름
        private String inviterName;   // 초대한 사람 이름
        private String roomLink;      // 채팅방 링크
    }
}
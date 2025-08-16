package com.example.demo.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class EmailDto {
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatInvite {
        private String email;       // 수신자 이메일
        private Long roomId;      // 방 ID
        private String roomName;    // 채팅방 이름
        private String inviterName; // 초대한 사람 이름
    }
}

import com.ssafy.ottereview.email.dto.EmailResponseDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailUtil {
    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;

    /**
     * 채팅방 초대 메일 발송
     * @param emailMessageDto 메일 수신자 및 기본 정보
     * @param type 템플릿 경로 ("email/chat_invite")
     */
    public void sendChatRoomInviteMail(EmailResponseDto.EmailMessage emailMessageDto, String type) {
        HashMap<String, String> map = new HashMap<>();
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        // 초대 메일 템플릿에 들어갈 데이터 설정
        map.put("inviterName", emailMessageDto.getInviterName()); // 초대한 사람
        map.put("roomName", emailMessageDto.getRoomName());       // 채팅방 이름
        map.put("roomLink", emailMessageDto.getRoomLink());       // 접속 링크

        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            mimeMessageHelper.setTo(emailMessageDto.getTo());        // 메일 수신자
            mimeMessageHelper.setSubject(emailMessageDto.getSubject()); // 메일 제목
            mimeMessageHelper.setText(setContext(map, type), true);  // 메일 본문 (HTML)
            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            log.error("sendChatRoomInviteMail error occurred!", e);
        }
    }

    // thymeleaf를 통한 html 템플릿 적용
    public String setContext(HashMap<String, String> map, String type) {
        Context context = new Context();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }
        return templateEngine.process(type, context);
    }
}
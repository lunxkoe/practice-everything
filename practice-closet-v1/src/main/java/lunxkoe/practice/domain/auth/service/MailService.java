package lunxkoe.practice.domain.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lunxkoe.practice.global.exception.BusinessException;
import lunxkoe.practice.global.exception.ErrorCode;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendPasswordResetEmail(String targetEmail, String tempPassword) {

        try {
            // 1. MimeMessage 생성
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            // 2. MimeMessageHelper 생성 (인코딩을 UTF-8로 설정)
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(targetEmail);
            helper.setSubject("[서비스명] 임시 비밀번호 발급 안내입니다.");

            // 3. 전송할 예쁜 HTML 코드 작성 (직접 작성하거나 파일에서 읽어오기)
            String htmlContent =
                    "<div style='margin: 50px; font-family: sans-serif;'>" +
                            "<h1>비밀번호 초기화 안내</h1>" +
                            "<br>" +
                            "<p>요청하신 임시 비밀번호가 발급되었습니다.</p>" +
                            "<p>아래 비밀번호로 로그인 후, 반드시 비밀번호를 변경해 주세요.</p>" +
                            "<div style='border:1px solid #ccc; padding: 20px; margin-top: 20px; font-size: 24px; font-weight: bold; color: #4CAF50;'>" +
                            tempPassword +
                            "</div>" +
                            "</div>";

            // 4. 핵심: 두 번째 인자를 true로 주면 HTML 양식으로 전송됨
            helper.setText(htmlContent, true);

            // 5. 메일 전송
            mailSender.send(mimeMessage);
            log.info("HTML 이메일 발송 완료: {}", targetEmail);

        } catch (MessagingException e) {
            // 메일 발송 실패 시 예외 처리 (CustomException 활용)
            log.error("이메일 발송 실패: {}", targetEmail, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}

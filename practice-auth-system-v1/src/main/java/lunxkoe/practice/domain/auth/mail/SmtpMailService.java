package lunxkoe.practice.domain.auth.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpMailService implements MailService{

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Override
    @Async("mailExecutor")
    public void sendTemporaryPassword(String toEmail, String temporaryPassword) {
        try {
            Context context = new Context();
            context.setVariable("temporaryPassword", temporaryPassword);
            context.setVariable("expireMinutes", 3);
            String html = templateEngine.process("mail/temporary-password", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("[옷장을 부탁해] 임시 비밀번호 안내");
            helper.setText(html, true);

            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            log.error("임시 비밀번호 메일 발송 실패. to={}", toEmail, e);
        }
    }
}

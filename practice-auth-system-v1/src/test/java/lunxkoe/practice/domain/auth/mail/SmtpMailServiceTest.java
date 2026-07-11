package lunxkoe.practice.domain.auth.mail;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpMailServiceTest {

    @Mock
    JavaMailSender mailSender;
    @Mock
    TemplateEngine templateEngine;

    @InjectMocks
    SmtpMailService smtpMailService;

    private MimeMessage realMimeMessage() {
        return new MimeMessage(Session.getDefaultInstance(new Properties()));
    }

    @Test
    void 정상_발송이면_예외없이_완료되고_템플릿에_임시비밀번호와_만료시간을_넘긴다() {
        given(mailSender.createMimeMessage()).willReturn(realMimeMessage());
        given(templateEngine.process(eq("mail/temporary-password"), any(Context.class))).willReturn("<html></html>");

        smtpMailService.sendTemporaryPassword("woody@example.com", "Temp1234!");

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("mail/temporary-password"), contextCaptor.capture());
        Context usedContext = contextCaptor.getValue();
        assertThatCode(() -> {
            assert usedContext.getVariable("temporaryPassword").equals("Temp1234!");
            assert usedContext.getVariable("expireMinutes").equals(3);
        }).doesNotThrowAnyException();

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void SMTP_발송_실패로_MailException이_발생해도_호출자에게_예외가_전파되지_않는다() {
        given(mailSender.createMimeMessage()).willReturn(realMimeMessage());
        given(templateEngine.process(eq("mail/temporary-password"), any(Context.class))).willReturn("<html></html>");
        willThrow(new MailSendException("SMTP 연결 실패")).given(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> smtpMailService.sendTemporaryPassword("woody@example.com", "Temp1234!"))
                .doesNotThrowAnyException();
    }
}

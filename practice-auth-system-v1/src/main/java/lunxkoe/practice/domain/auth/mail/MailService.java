package lunxkoe.practice.domain.auth.mail;

public interface MailService {
    void sendTemporaryPassword(String toEmail, String temporaryPassword);
}

package lunxkoe.practice.global.security;

import lunxkoe.practice.global.jwt.UserPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UUID getCurrentUserId() {
        return getPrincipal().userId();
    }

    public static UserPrincipal getPrincipal() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            throw new IllegalStateException("인증되지 않은 컨텍스트입니다.");
        }
        return userPrincipal;
    }
}

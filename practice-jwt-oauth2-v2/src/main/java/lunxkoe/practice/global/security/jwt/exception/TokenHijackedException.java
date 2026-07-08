package lunxkoe.practice.global.security.jwt.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class TokenHijackedException extends RuntimeException{
    private final UUID userId;
}

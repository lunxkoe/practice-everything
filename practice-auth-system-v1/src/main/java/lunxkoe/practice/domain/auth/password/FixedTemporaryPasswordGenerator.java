package lunxkoe.practice.domain.auth.password;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"demo"})
public class FixedTemporaryPasswordGenerator implements TemporaryPasswordGenerator {

    @Override
    public String generate() {
        return "temporary1!!";
    }
}

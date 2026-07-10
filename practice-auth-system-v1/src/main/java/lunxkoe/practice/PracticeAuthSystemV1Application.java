package lunxkoe.practice;

import lunxkoe.practice.global.jwt.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({JwtProperties.class})
@SpringBootApplication
public class PracticeAuthSystemV1Application {

    public static void main(String[] args) {
        SpringApplication.run(PracticeAuthSystemV1Application.class, args);
    }

}

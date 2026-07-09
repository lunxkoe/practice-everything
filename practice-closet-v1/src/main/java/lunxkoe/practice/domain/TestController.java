package lunxkoe.practice.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class TestController {

    @GetMapping()
    public String mainP() {
        log.info("mainP");
        return "mainP";
    }

    @GetMapping("/my")
    public String myP() {
        log.info("myP");
        return "myP";
    }

    @GetMapping("admin")
    public String adminP() {
        log.info("adminP");
        return "adminP";
    }
}

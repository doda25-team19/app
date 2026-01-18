package frontend.ctrl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HelloWorldController {

    @Value("${APP_VERSION:unknown}")
    private String appVersion;

    @GetMapping("/")
    @ResponseBody
    public String index() {
       return String.format("Hello World! [Version: %s]", appVersion);
    }
}
package rs.majic.de.nuc.megaskins.megaskins.endpoint;

import ch.qos.logback.core.model.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class FrontendController {

    @GetMapping(value="/greeting")
    public @ResponseBody String greeting(@RequestParam(name="name", required=false, defaultValue="World") String name, Model model) {
        return "Hello " + name;
    }
}

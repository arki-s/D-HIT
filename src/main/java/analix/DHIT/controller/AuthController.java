package analix.DHIT.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
public class AuthController {

    @GetMapping("/")
    public String defaultRouting() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/manager/home";
//            return "/common";
        } else if (authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("ROLE_USER"))) {
            return "redirect:/member/report/create";
//            return "/common";
        }
        return "redirect:/login";

    }

    @GetMapping("/login")
    public String displayLogin() {
        return "common/login";
    }

}

package com.carsale.erp.account;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.carsale.erp.security.CustomUserDetails;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal CustomUserDetails principal) {
        if (principal != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }
}

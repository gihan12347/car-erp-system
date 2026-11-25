package com.carsale.erp.shared.settings;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class BusinessProfileAdvice {

    private final BusinessProfileService businessProfileService;

    @ModelAttribute("businessProfile")
    public BusinessProfile businessProfile() {
        return businessProfileService.current();
    }
}

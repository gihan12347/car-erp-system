package com.carsale.erp.shared.settings;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class SalesContactAdvice {

    private final SalesContactService salesContactService;

    @ModelAttribute("salesContact")
    public SalesContact salesContact() {
        return salesContactService.current();
    }
}

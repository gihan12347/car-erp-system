package com.carsale.erp.config;

import com.carsale.erp.customer.PersonalImportLinkInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final PersonalImportLinkInterceptor personalImportLinkInterceptor;

    public WebMvcConfig(PersonalImportLinkInterceptor personalImportLinkInterceptor) {
        this.personalImportLinkInterceptor = personalImportLinkInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(personalImportLinkInterceptor)
                .addPathPatterns(
                        "/auction",
                        "/equipment/new",
                        "/export/new",
                        "/standards/new",
                        "/jevic/new",
                        "/shipping/new",
                        "/grade/new",
                        "/coi/new",
                        "/photos/new",
                        "/vehicle/basics"
                );
    }
}

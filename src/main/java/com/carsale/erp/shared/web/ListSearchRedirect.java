package com.carsale.erp.shared.web;

import java.nio.charset.StandardCharsets;

import org.springframework.web.util.UriUtils;

public final class ListSearchRedirect {

    private ListSearchRedirect() {
    }

    public static String to(String listPath, String query) {
        if (query != null && !query.trim().isEmpty()) {
            return "redirect:" + listPath + "?q=" + UriUtils.encodeQueryParam(query.trim(), StandardCharsets.UTF_8);
        }
        return "redirect:" + listPath;
    }
}

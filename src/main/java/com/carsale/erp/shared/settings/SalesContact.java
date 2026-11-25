package com.carsale.erp.shared.settings;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public final class SalesContact {

    private final String phone;
    private final String whatsapp;

    public SalesContact(String phone, String whatsapp) {
        this.phone = phone;
        this.whatsapp = whatsapp;
    }

    public static SalesContact empty() {
        return new SalesContact(null, null);
    }

    public String getPhone() {
        return phone;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public boolean isHasPhone() {
        return phone != null;
    }

    public boolean isHasWhatsapp() {
        return whatsapp != null;
    }

    public boolean isHasAny() {
        return isHasPhone() || isHasWhatsapp();
    }

    public String getTelHref() {
        return phone == null ? null : "tel:" + phone.replaceAll("[^+0-9]", "");
    }

    public String whatsappLink(String title, String stockNo) {
        String digits = whatsappDigits(whatsapp);
        if (digits == null) {
            return null;
        }
        String name = title == null || title.trim().isEmpty() ? "this vehicle" : title.trim();
        String message = "Hi, I'm interested in the " + name;
        if (stockNo != null && !stockNo.trim().isEmpty()) {
            message = message + " (Stock: " + stockNo.trim() + ")";
        }
        message = message + ". Could you please provide more details?";
        return "https://wa.me/" + digits + "?text=" + encode(message);
    }

    static String whatsappDigits(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return null;
        }
        if (digits.startsWith("0")) {
            digits = "94" + digits.substring(1);
        }
        return digits;
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException ex) {
            return value;
        }
    }
}

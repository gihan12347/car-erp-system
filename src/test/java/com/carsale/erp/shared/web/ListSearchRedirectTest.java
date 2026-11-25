package com.carsale.erp.shared.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ListSearchRedirectTest {

    @Test
    void redirectsToTheListWhenTheQueryIsBlank() {
        assertEquals("redirect:/import", ListSearchRedirect.to("/import", null));
        assertEquals("redirect:/customs", ListSearchRedirect.to("/customs", "   "));
    }

    @Test
    void keepsTheTrimmedSearchOnTheSameList() {
        assertEquals("redirect:/import?q=abc", ListSearchRedirect.to("/import", " abc "));
        assertEquals("redirect:/customs?q=a%20b", ListSearchRedirect.to("/customs", "a b"));
    }
}

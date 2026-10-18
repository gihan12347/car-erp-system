package com.carsale.erp.customer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class PersonalImportLinkInterceptorTest {

    @Test
    void linksOnlyNewImportSubmits() {
        MockHttpServletRequest auction = new MockHttpServletRequest("POST", "/auction");
        auction.setParameter("editMode", "false");
        assertTrue(PersonalImportLinkInterceptor.isNewImport(auction));

        MockHttpServletRequest edit = new MockHttpServletRequest("POST", "/auction");
        edit.setParameter("editMode", "true");
        assertFalse(PersonalImportLinkInterceptor.isNewImport(edit));

        assertTrue(PersonalImportLinkInterceptor.isNewImport(new MockHttpServletRequest("POST", "/equipment/new")));
        assertTrue(PersonalImportLinkInterceptor.isNewImport(new MockHttpServletRequest("POST", "/vehicle/basics")));
        assertFalse(PersonalImportLinkInterceptor.isNewImport(new MockHttpServletRequest("POST", "/equipment/upload-document")));
    }
}

package com.carsale.erp.shared.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ListPagingTest {

    @Test
    void slicesTenNewestCompatibleRowsPerPage() {
        List<Integer> rows = numbers(24);
        ListPaging.Window<Integer> first = ListPaging.of(rows, "1");
        assertEquals(10, first.getItems().size());
        assertEquals(Integer.valueOf(1), first.getItems().get(0));
        assertEquals(Integer.valueOf(10), first.getItems().get(9));
        assertEquals(3, first.getPages());
        assertEquals("1–10 of 24", first.getRange());

        ListPaging.Window<Integer> last = ListPaging.of(rows, "3");
        assertEquals(4, last.getItems().size());
        assertEquals(Integer.valueOf(21), last.getItems().get(0));
        assertEquals("21–24 of 24", last.getRange());
    }

    @Test
    void clampsInvalidPages() {
        List<Integer> rows = numbers(11);
        assertEquals(1, ListPaging.of(rows, "0").getPage());
        assertEquals(2, ListPaging.of(rows, "99").getPage());
        assertEquals(1, ListPaging.of(rows, "nope").getPage());
        assertTrue(ListPaging.of(new ArrayList<Integer>(), "2").getItems().isEmpty());
    }

    private static List<Integer> numbers(int count) {
        List<Integer> rows = new ArrayList<Integer>();
        for (int i = 1; i <= count; i++) {
            rows.add(Integer.valueOf(i));
        }
        return rows;
    }
}

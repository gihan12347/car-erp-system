package com.carsale.erp.shared.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import org.springframework.ui.Model;

public final class ListPaging {

    public static final int PAGE_SIZE = 10;

    private ListPaging() {
    }

    public static <T> List<T> apply(List<T> items, String page, Model model, String listPath) {
        Window<T> window = of(items, page);
        window.addTo(model);
        model.addAttribute("listPath", listPath);
        return window.getItems();
    }

    public static <T> Window<T> of(List<T> items, String page) {
        List<T> source = items == null ? Collections.<T>emptyList() : items;
        int total = source.size();
        int pages = pageCount(total);
        int current = clampPage(parsePage(page), pages);
        return new Window<T>(slice(source, current), current, pages, total, pageNumbers(current, pages), pageRange(current, total));
    }

    static int parsePage(String page) {
        if (page == null || page.trim().isEmpty()) {
            return 1;
        }
        try {
            return Integer.parseInt(page.trim());
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    static int pageCount(int total) {
        if (total <= 0) {
            return 0;
        }
        return (total + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    static int clampPage(int page, int pages) {
        if (pages <= 0) {
            return 1;
        }
        if (page < 1) {
            return 1;
        }
        if (page > pages) {
            return pages;
        }
        return page;
    }

    static <T> List<T> slice(List<T> items, int page) {
        int total = items.size();
        if (total == 0) {
            return items;
        }
        int safePage = clampPage(page, pageCount(total));
        int from = (safePage - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);
        return new ArrayList<T>(items.subList(from, to));
    }

    static String pageRange(int page, int total) {
        if (total <= 0) {
            return "";
        }
        int from = (page - 1) * PAGE_SIZE + 1;
        int to = Math.min(page * PAGE_SIZE, total);
        return from + "–" + to + " of " + total;
    }

    static List<Integer> pageNumbers(int page, int pages) {
        List<Integer> numbers = new ArrayList<Integer>();
        if (pages <= 0) {
            return numbers;
        }
        if (pages <= 7) {
            for (int number = 1; number <= pages; number++) {
                numbers.add(Integer.valueOf(number));
            }
            return numbers;
        }
        numbers.add(Integer.valueOf(1));
        int start = Math.max(2, page - 1);
        int end = Math.min(pages - 1, page + 1);
        if (page <= 3) {
            start = 2;
            end = 4;
        } else if (page >= pages - 2) {
            start = pages - 3;
            end = pages - 1;
        }
        if (start > 2) {
            numbers.add(Integer.valueOf(0));
        }
        for (int number = start; number <= end; number++) {
            numbers.add(Integer.valueOf(number));
        }
        if (end < pages - 1) {
            numbers.add(Integer.valueOf(0));
        }
        numbers.add(Integer.valueOf(pages));
        return numbers;
    }

    @Getter
    public static final class Window<T> {
        private final List<T> items;
        private final int page;
        private final int pages;
        private final int total;
        private final List<Integer> numbers;
        private final String range;

        Window(List<T> items, int page, int pages, int total, List<Integer> numbers, String range) {
            this.items = items;
            this.page = page;
            this.pages = pages;
            this.total = total;
            this.numbers = numbers;
            this.range = range;
        }

        public void addTo(Model model) {
            model.addAttribute("listPage", Integer.valueOf(page));
            model.addAttribute("listPages", Integer.valueOf(pages));
            model.addAttribute("listPageNumbers", numbers);
            model.addAttribute("listRange", range);
            model.addAttribute("listTotal", Integer.valueOf(total));
        }
    }
}

package com.carsale.erp.customer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PersonalImportLinkInterceptor implements HandlerInterceptor {

    private final PersonalImportLinker linker;

    public PersonalImportLinkInterceptor(PersonalImportLinker linker) {
        this.linker = linker;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        if (ex != null || !"POST".equalsIgnoreCase(request.getMethod()) || !isNewImport(request)) {
            return;
        }
        linker.linkPending(request.getParameter("chassisNo"));
    }

    static boolean isNewImport(HttpServletRequest request) {
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }
        if ("/auction".equals(path)) {
            return !"true".equalsIgnoreCase(request.getParameter("editMode"));
        }
        return "/equipment/new".equals(path)
                || "/export/new".equals(path)
                || "/standards/new".equals(path)
                || "/jevic/new".equals(path)
                || "/shipping/new".equals(path)
                || "/grade/new".equals(path)
                || "/coi/new".equals(path)
                || "/photos/new".equals(path)
                || "/vehicle/basics".equals(path);
    }
}

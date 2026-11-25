package com.carsale.erp.shared.share;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import com.carsale.erp.importpipeline.model.VehiclePhoto;
import com.carsale.erp.shared.document.InlineDocumentResponse;
import com.carsale.erp.shared.document.SheetDocumentStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class VehicleShareController {

    private static final int PUBLIC_LIMIT = 80;
    private static final long PUBLIC_WINDOW_MS = 60_000L;

    private final VehicleShareService vehicleShareService;
    private final SheetDocumentStorageService documentStorageService;
    private final Map<String, Window> publicHits = new ConcurrentHashMap<String, Window>();

    @PostMapping("/vehicle/{chassisNo}/share")
    public Object share(
            @PathVariable String chassisNo,
            @RequestParam(value = "returnTo", required = false) String returnTo,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        boolean json = acceptsJson(request);
        try {
            String token = vehicleShareService.ensureLink(chassisNo);
            if (json) {
                Map<String, String> body = new java.util.LinkedHashMap<String, String>();
                body.put("url", vehicleShareService.publicUrl(request, token));
                return ResponseEntity.ok(body);
            }
            redirectAttributes.addFlashAttribute("shareToken", token);
            redirectAttributes.addFlashAttribute("shareNotice", "Sharing link is ready.");
            return "redirect:" + safeReturn(returnTo, chassisNo);
        } catch (IllegalArgumentException ex) {
            if (json) {
                Map<String, String> body = new java.util.LinkedHashMap<String, String>();
                body.put("error", ex.getMessage());
                return ResponseEntity.badRequest().body(body);
            }
            redirectAttributes.addFlashAttribute("shareError", ex.getMessage());
            return "redirect:" + safeReturn(returnTo, chassisNo);
        }
    }

    @GetMapping("/shared/vehicle/{token}")
    public String publicPage(@PathVariable String token, HttpServletRequest request, Model model) {
        limit(request);
        model.addAttribute("pageTitle", "Vehicle");
        if (!vehicleShareService.linkExists(token)) {
            model.addAttribute("shareState", "missing");
            return "shared/vehicle";
        }
        PublicVehicleView vehicle = vehicleShareService.publicView(token);
        if (vehicle == null) {
            model.addAttribute("shareState", "unavailable");
            return "shared/vehicle";
        }
        model.addAttribute("shareState", "ready");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("pageTitle", vehicle.getTitle());
        return "shared/vehicle";
    }

    @GetMapping("/shared/vehicle/{token}/photos/{storedName:.+}")
    public ResponseEntity<Resource> photo(
            @PathVariable String token,
            @PathVariable String storedName,
            HttpServletRequest request
    ) throws IOException {
        limit(request);
        VehiclePhoto photo = vehicleShareService.publicPhoto(token, storedName);
        if (photo == null) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = documentStorageService.loadPhotoAsResource(photo.getStoredName());
        String contentType = documentStorageService.resolveContentType(photo.getStoredName(), photo.getContentType());
        return InlineDocumentResponse.of(resource, photo.getStoredName(), contentType);
    }

    private void limit(HttpServletRequest request) {
        String key = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
        long now = System.currentTimeMillis();
        Window window = publicHits.get(key);
        if (window == null || now - window.started > PUBLIC_WINDOW_MS) {
            publicHits.put(key, new Window(now, 1));
            return;
        }
        window.count++;
        if (window.count > PUBLIC_LIMIT) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests.");
        }
    }

    private static boolean acceptsJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.toLowerCase().contains("application/json");
    }

    static String safeReturn(String returnTo, String chassisNo) {
        String fallback = "/vehicle/" + (chassisNo == null ? "" : chassisNo.trim());
        if (returnTo == null) {
            return fallback;
        }
        String path = returnTo.trim();
        if (path.indexOf("://") >= 0 || path.indexOf("//") >= 0 || path.indexOf('\\') >= 0 || path.indexOf('?') >= 0) {
            return fallback;
        }
        if (path.startsWith("/vehicle/") || path.startsWith("/ready-for-sale/") || path.startsWith("/yards/")) {
            return path;
        }
        return fallback;
    }

    private static final class Window {
        private final long started;
        private int count;

        private Window(long started, int count) {
            this.started = started;
            this.count = count;
        }
    }
}

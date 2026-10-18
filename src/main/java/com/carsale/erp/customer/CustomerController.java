package com.carsale.erp.customer;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.carsale.erp.customer.CustomerService.CustomerRecord;
import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.SheetDocumentStorageService;
import com.carsale.erp.shared.document.SheetUploadResult;
import com.carsale.erp.shared.document.document.IdCardParser;
import com.carsale.erp.shared.ocr.OcrClient;
import com.carsale.erp.shared.ocr.OcrImagePreparer;
import com.carsale.erp.shared.service.IntakePlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final PersonalImportLinker personalImportLinker;
    private final IntakePlanService intakePlanService;
    private final SheetDocumentStorageService documentStorageService;
    private final IdCardParser parser;
    private final OcrImagePreparer imagePreparer;

    @GetMapping("/customers")
    public String list(@RequestParam(value = "q", required = false) String query, Model model) {
        model.addAttribute("pageTitle", "Customers");
        model.addAttribute("activeMenu", "customers");
        model.addAttribute("searchQuery", query == null ? "" : query);
        model.addAttribute("rows", customerService.list(query));
        return "customers/list";
    }

    @GetMapping("/customers/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("customer")) {
            model.addAttribute("customer", new Customer());
        }
        model.addAttribute("pageTitle", "New customer");
        model.addAttribute("activeMenu", "customers");
        return "customers/new";
    }

    @PostMapping("/customers/new")
    public String create(
            @ModelAttribute("customer") Customer customer,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Customer saved = customerService.savePersonal(customer);
            personalImportLinker.remember(saved.getId());
            return "redirect:" + intakePlanService.afterCustomerPath(saved.getId());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("customer", customer);
            return "redirect:/customers/new";
        }
    }

    @GetMapping("/customers/personal/{id}")
    public String personalDetail(@PathVariable Long id, Model model) {
        CustomerRecord customer = customerService.findPersonal(id);
        if (customer == null) {
            return "redirect:/customers";
        }
        model.addAttribute("pageTitle", customer.getFullName() == null ? "Customer" : customer.getFullName());
        model.addAttribute("activeMenu", "customers");
        model.addAttribute("customer", customer);
        return "customers/detail";
    }

    @GetMapping("/customers/{role}/{chassisNo}")
    public String detail(
            @PathVariable String role,
            @PathVariable String chassisNo,
            Model model
    ) {
        CustomerRecord customer = customerService.find(role, chassisNo);
        if (customer == null) {
            return "redirect:/customers";
        }
        model.addAttribute("pageTitle", customer.getFullName() == null ? "Customer" : customer.getFullName());
        model.addAttribute("activeMenu", "customers");
        model.addAttribute("customer", customer);
        return "customers/detail";
    }

    @GetMapping("/customers/documents/{storedName:.+}")
    public ResponseEntity<Resource> serveDocument(@PathVariable String storedName) throws IOException {
        Resource resource = documentStorageService.loadIdCardAsResource(storedName);
        String contentType = documentStorageService.resolveContentType(storedName, null);
        String encodedName = URLEncoder.encode(storedName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @PostMapping("/customers/upload-document")
    @ResponseBody
    public SheetUploadResult uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            return documentStorageService.storeIdCard(file);
        } catch (IOException ex) {
            SheetUploadResult failed = new SheetUploadResult();
            failed.setSuccess(false);
            failed.setMessage("Could not save the identity card. " + ex.getMessage());
            return failed;
        }
    }

    @PostMapping("/customers/parse-id")
    @ResponseBody
    public AuctionParseResult parseId(@RequestParam("file") MultipartFile file) {
        return imagePreparer.parsePage(file, parser, OcrClient.GOOGLE);
    }
}

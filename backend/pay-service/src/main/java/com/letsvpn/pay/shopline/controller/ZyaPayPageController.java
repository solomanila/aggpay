package com.letsvpn.pay.shopline.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/pay/shopline/page")
public class ZyaPayPageController {

    @GetMapping(value = "/privacy-policy", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> privacyPolicy() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/privacy-policy.html");
        String html = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        return ResponseEntity.ok(html);
    }

    @GetMapping(value = "/faq", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> faq() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/faq.html");
        String html = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        return ResponseEntity.ok(html);
    }
}

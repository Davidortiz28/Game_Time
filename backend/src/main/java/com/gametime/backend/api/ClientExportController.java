package com.gametime.backend.api;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gametime.backend.common.BusinessRuleException;

@RestController
@RequestMapping("/api/client/exports")
public class ClientExportController {

    private final ExportService exports;

    public ClientExportController(ExportService exports) {
        this.exports = exports;
    }

    @PostMapping
    public ExportRequest request() {
        return exports.request();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"gametime-sessions.csv\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(exports.download(id));
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<Map<String, String>> rejected(BusinessRuleException exception) {
        return ResponseEntity.status(409).body(Map.of("message", exception.getMessage()));
    }
}

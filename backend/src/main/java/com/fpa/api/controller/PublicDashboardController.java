package com.fpa.api.controller;

import com.fpa.api.dto.PublicDashboardResponse;
import com.fpa.service.PublicDashboardService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/dashboard")
public class PublicDashboardController {

    private final PublicDashboardService publicDashboardService;

    public PublicDashboardController(PublicDashboardService publicDashboardService) {
        this.publicDashboardService = publicDashboardService;
    }

    @GetMapping
    public ResponseEntity<PublicDashboardResponse> getDashboard() {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noCache())
            .body(publicDashboardService.getDashboard());
    }
}

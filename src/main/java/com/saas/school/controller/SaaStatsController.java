package com.saas.school.controller;

import com.saas.school.service.SaasStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/superadmin/stats")
@RequiredArgsConstructor
public class SaaStatsController {

    private final SaasStatsService service;

   // @GetMapping
    //public Map<String, Object> getStats() {
      //  return service.getGlobalStats();
    //}

    @GetMapping
    public Map<String, Object> getStats() {
        return service.getFullStats();
    }
}


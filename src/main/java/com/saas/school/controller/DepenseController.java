package com.saas.school.controller;

import com.saas.school.dto.DepenseDTO;
import com.saas.school.service.DepenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/depenses")
@RequiredArgsConstructor
public class DepenseController {

    private final DepenseService depenseService;

    @GetMapping("/ecole/{ecoleId}")
    public ResponseEntity<List<DepenseDTO>> getDepenses(
            @PathVariable Long ecoleId
    ) {
        return ResponseEntity.ok(
                depenseService.findByEcole(ecoleId)
        );
    }

    @PostMapping("/ecole/{ecoleId}")
    public ResponseEntity<DepenseDTO> creer(
            @PathVariable Long ecoleId,
            @RequestBody DepenseDTO dto
    ) {
        return ResponseEntity.ok(
                depenseService.creer(ecoleId, dto)
        );
    }
}
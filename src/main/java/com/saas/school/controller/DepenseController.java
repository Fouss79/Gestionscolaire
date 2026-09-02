package com.saas.school.controller;

import com.saas.school.dto.DepenseDTO;
import com.saas.school.service.DepenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/depenses")
@RequiredArgsConstructor
@CrossOrigin("*")
public class DepenseController {

    private final DepenseService depenseService;

    @PostMapping("/ecole/{ecoleId}")
    public DepenseDTO creer(@PathVariable Long ecoleId, @RequestBody DepenseDTO dto) {
        return depenseService.creer(ecoleId, dto);
    }

    @GetMapping("/ecole/{ecoleId}")
    public List<DepenseDTO> getByEcole(@PathVariable Long ecoleId) {
        return depenseService.findByEcole(ecoleId);
    }

    @GetMapping("/{id}")
    public DepenseDTO getById(@PathVariable Long id) {
        return depenseService.getByIdDTO(id);
    }
}
package com.saas.school.controller;

import com.saas.school.dto.BesoinHeureDTO;
import com.saas.school.dto.BesoinHeureRequestDTO;
import com.saas.school.service.BesoinHeureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/besoin-heures")
@RequiredArgsConstructor
@CrossOrigin("*")
public class BesoinHeureController {

    private final BesoinHeureService service;

    // ✅ CREATE
    @PostMapping
    public BesoinHeureDTO create(@RequestBody BesoinHeureRequestDTO dto) {
        return service.create(dto);
    }

    // ✅ GET ALL
    @GetMapping
    public List<BesoinHeureDTO> getAll() {
        return service.getAll();
    }

    // ✅ GET BY ID
    @GetMapping("/{id}")
    public BesoinHeureDTO getById(@PathVariable Long id) {
        return service.getById(id);
    }

    // ✅ UPDATE
    @PutMapping("/{id}")
    public BesoinHeureDTO update(
            @PathVariable Long id,
            @RequestBody BesoinHeureRequestDTO dto
    ) {
        return service.update(id, dto);
    }

    // ✅ DELETE
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "Supprimé avec succès ✅";
    }
}
package com.saas.school.controller;

import com.saas.school.entity.TypeFrais;
import com.saas.school.service.TypeFraisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/type-frais")
@RequiredArgsConstructor
@CrossOrigin("*")
public class TypeFraisController {

    private final TypeFraisService typeFraisService;

    @PostMapping("/ecole/{ecoleId}")
    public TypeFrais creer(
            @PathVariable Long ecoleId,
            @RequestBody TypeFrais typeFrais
    ) {
        return typeFraisService.creer(ecoleId, typeFrais);
    }

    @GetMapping("/ecole/{ecoleId}")
    public List<TypeFrais> getByEcole(
            @PathVariable Long ecoleId
    ) {
        return typeFraisService.getByEcole(ecoleId);
    }

    @GetMapping("/{id}")
    public TypeFrais getById(
            @PathVariable Long id
    ) {
        return typeFraisService.getById(id);
    }

    @DeleteMapping("/{id}")
    public void supprimer(
            @PathVariable Long id
    ) {
        typeFraisService.supprimer(id);
    }
}
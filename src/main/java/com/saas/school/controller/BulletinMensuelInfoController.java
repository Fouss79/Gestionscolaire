package com.saas.school.controller;

import com.saas.school.dto.InfoDto;
import com.saas.school.dto.InfosRequest;
import com.saas.school.service.BulletinMensuelInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bulletins-mensuels")
@RequiredArgsConstructor
public class BulletinMensuelInfoController {

    private final BulletinMensuelInfoService service;

    @GetMapping("/infos")
    public List<InfoDto> infos(@RequestParam Long classeId,
                               @RequestParam Long anneeId,
                               @RequestParam String mois) {
        return service.lister(classeId, anneeId, mois);
    }

    @PutMapping("/infos")
    public ResponseEntity<Void> enregistrer(@Valid @RequestBody InfosRequest request) {
        service.enregistrer(request);
        return ResponseEntity.noContent().build();
    }
}

package com.saas.school.controller;

import com.saas.school.dto.MatiereDTO;
import com.saas.school.entity.Matiere;
import com.saas.school.repository.MatiereRepository;
import com.saas.school.service.MatiereService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/matieres")
@RequiredArgsConstructor
@CrossOrigin
public class MatiereController {

    private final MatiereService matiereService;
  private final MatiereRepository repository;
    @PostMapping
    public Matiere create(@RequestBody MatiereDTO request) {
        return matiereService.create(request.getNom(), request.getEcoleId());
    }

    @GetMapping("/ecole/{ecoleId}")
    public List<Matiere> getByEcole(@PathVariable Long ecoleId) {
        return matiereService.getByEcole(ecoleId);
    }


    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
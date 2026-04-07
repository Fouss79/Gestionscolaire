package com.saas.school.controller;
import com.saas.school.dto.EleveResponseDTO;
import com.saas.school.dto.InscriptionDTO;
import com.saas.school.dto.InscriptionRequest;
import com.saas.school.dto.InscriptionResponseDTO;
import com.saas.school.entity.Classe;
import com.saas.school.entity.Eleve;
import com.saas.school.entity.Inscription;
import com.saas.school.repository.ClasseRepository;
import com.saas.school.repository.EleveRepository;
import com.saas.school.repository.InscriptionRepository;
import com.saas.school.service.InscriptionService;
import com.saas.school.service.NiveauService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inscriptions")
@RequiredArgsConstructor

public class InscriptionController {
    private final InscriptionService inscriptionService;
    private final InscriptionRepository inscriptionRepository;
    private final EleveRepository eleveRepository;
    private final ClasseRepository classeRepository;
    //@PostMapping
    //public ResponseEntity<Inscription> inscrire(@RequestBody InscriptionRequest request) {
      //  return ResponseEntity.ok(inscriptionService.inscrireEleve(request));
    //}
    @PostMapping
    public ResponseEntity<Inscription> inscrire(@RequestBody InscriptionDTO request) {
        return ResponseEntity.ok(inscriptionService.inscrireUnEleve(request));
    }

    @GetMapping
    public ResponseEntity<List<EleveResponseDTO>> getEleves() {
        return ResponseEntity.ok(inscriptionService.getAllEleves());
    }
    @GetMapping("/classe/{classeId}/annee/{anneeId}")
    public List<Inscription> getByClasseAndAnnee(
            @PathVariable Long classeId,
            @PathVariable Long anneeId) {

        return inscriptionRepository
                .findByClasseIdAndAnneeScolaire_Id(classeId, anneeId);
    }
    @GetMapping("/ecole/{ecoleId}/active")
    public List<InscriptionResponseDTO> getByEcoleAndAnneeActive(@PathVariable Long ecoleId) {
        return inscriptionService.getInscriptionsByEcoleAndAnneeActive(ecoleId);
    }


}

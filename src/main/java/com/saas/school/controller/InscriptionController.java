package com.saas.school.controller;
import com.saas.school.dto.*;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    @PutMapping("/{id}/valider")
    public ResponseEntity<InscriptionResponseDTO> valider(@PathVariable Long id) {
        Inscription inscription = inscriptionService.validerInscription(id);
        return ResponseEntity.ok(inscriptionService.getById(inscription.getId()));
    }

    @PutMapping("/{id}/rejeter")
    public ResponseEntity<InscriptionResponseDTO> rejeter(@PathVariable Long id) {
        Inscription inscription = inscriptionService.rejeterInscription(id);
        return ResponseEntity.ok(inscriptionService.getById(inscription.getId()));
    }
    @GetMapping("/ecole/{ecoleId}/reinscription")
    public ResponseEntity<List<ReinscriptionReponseDTO>> getElevesPourReinscription(
            @PathVariable Long ecoleId
    ) {
        List<ReinscriptionReponseDTO> data =
                inscriptionService.getElevesPourReinscription(ecoleId);

        if (data == null) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(
                Optional.ofNullable(
                        inscriptionService.getElevesPourReinscription(ecoleId)
                ).orElse(List.of())
        );
    }

    // Dans PresenceService ou un service dédié
    public List<Map<String, Object>> getElevesAvecInscription(Long classeId) {
        return inscriptionRepository.findByClasseIdAndAnneeScolaire_ActiveTrue(classeId)
                .stream()
                .map(i -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("inscriptionId", i.getId());
                    m.put("eleveId", i.getEleve().getId());
                    m.put("nom", i.getEleve().getNom());
                    m.put("prenom", i.getEleve().getPrenom());
                    return m;
                })
                .toList();
    }


    @GetMapping
    public ResponseEntity<List<EleveResponseDTO>> getEleves() {
        return ResponseEntity.ok(inscriptionService.getAllEleves());
    }
    @GetMapping("/actif/classe/{classeId}/annee/{anneeId}")
    public List<InscriptionResponseDTO> getByClasseEtAnnee(
            @PathVariable Long classeId,
            @PathVariable Long anneeId) {

        System.out.println("=== ENDPOINT INSCRIPTION APPELE ===");

        return inscriptionService.getByClasseEtAnnee(classeId, anneeId);
    }
    @GetMapping("/classe/{classeId}/annee/{anneeId}")
    public List<InscriptionResponseDTO> getByClasseAndAnnee(
            @PathVariable Long classeId,
            @PathVariable Long anneeId) {

        return inscriptionService
                .getByClasseAndAnnee(classeId, anneeId);
    }


    @GetMapping("/ecole/{ecoleId}/active")
    public List<InscriptionResponseDTO> getByEcoleAndAnneeActive(@PathVariable Long ecoleId) {
        return inscriptionService.getInscriptionsByEcoleAndAnneeActive(ecoleId);
    }
    @PostMapping("/{inscriptionId}/reinscrire/{classeId}")
    public ResponseEntity<?> reinscrire(
            @PathVariable Long inscriptionId,
            @PathVariable Long classeId) {

        inscriptionService.reinscrire(inscriptionId, classeId);

        return ResponseEntity.ok("Réinscription effectuée");
    }
    @GetMapping("/classe/{classeId}/eleves-actifs")
    public List<EleveDTO> getElevesActifs(
            @PathVariable Long classeId
    ) {
        return inscriptionService.getElevesClasseActive(classeId);
    }
    @GetMapping("/{id}")
    public ResponseEntity<InscriptionResponseDTO> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                inscriptionService.getById(id)
        );
    }
    @PutMapping("/{id}")
    public ResponseEntity<InscriptionResponseDTO> modifier(
            @PathVariable Long id,
            @RequestBody InscriptionDTO request
    ) {

        Inscription inscription =
                inscriptionService.modifierInscription(id, request);

        return ResponseEntity.ok(
                inscriptionService.getById(inscription.getId())
        );
    }

}

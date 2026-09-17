package com.saas.school.controller;

import com.saas.school.dto.ExamenDTO;
import com.saas.school.dto.RepartitionExamenDTO;
import com.saas.school.dto.RepartitionExamenRequest;
import com.saas.school.entity.AnneeScolaire;
import com.saas.school.entity.Ecole;
import com.saas.school.entity.Examen;
import com.saas.school.repository.AnneeScolaireRepository;
import com.saas.school.repository.EcoleRepository;
import com.saas.school.repository.ExamenRepository;
import com.saas.school.service.RepartitionExamenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/examens")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ExamenController {

    private final ExamenRepository examenRepository;
    private final EcoleRepository ecoleRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;
    private final RepartitionExamenService repartitionExamenService;


    // =========================================================
    // EXAMENS
    // =========================================================

    /**
     * Créer un examen.
     *
     * Le frontend envoie ecole/anneeScolaire sous forme d'objets
     * { "id": ... }, mais on ne fait confiance qu'aux IDs, pas au
     * reste de l'objet potentiellement envoyé par le client.
     */
    @PostMapping
    public ResponseEntity<ExamenDTO> creerExamen(
            @RequestBody Examen examen
    ) {

        if (examen.getEcole() == null || examen.getEcole().getId() == null) {
            throw new RuntimeException("L'école est obligatoire.");
        }

        if (examen.getAnneeScolaire() == null
                || examen.getAnneeScolaire().getId() == null) {
            throw new RuntimeException("L'année scolaire est obligatoire.");
        }

        Ecole ecole = ecoleRepository.findById(examen.getEcole().getId())
                .orElseThrow(() -> new RuntimeException("École introuvable."));

        AnneeScolaire anneeScolaire = anneeScolaireRepository
                .findById(examen.getAnneeScolaire().getId())
                .orElseThrow(() -> new RuntimeException("Année scolaire introuvable."));

        if (anneeScolaire.getEcole() == null
                || !ecole.getId().equals(anneeScolaire.getEcole().getId())) {
            throw new RuntimeException(
                    "Cette année scolaire n'appartient pas à cette école."
            );
        }

        examen.setEcole(ecole);
        examen.setAnneeScolaire(anneeScolaire);

        Examen sauvegarde = examenRepository.save(examen);

        return ResponseEntity.ok(toDto(sauvegarde));
    }


    /**
     * Liste des examens d'une école pour une année scolaire.
     */
    @GetMapping("/ecole/{ecoleId}")
    public ResponseEntity<List<ExamenDTO>> getExamens(
            @PathVariable Long ecoleId,
            @RequestParam Long anneeId
    ) {

        List<ExamenDTO> examens = examenRepository
                .findByEcoleIdAndAnneeScolaireIdOrderByDateExamenAsc(
                        ecoleId,
                        anneeId
                )
                .stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(examens);
    }


    /**
     * Récupérer un examen.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExamenDTO> getExamen(
            @PathVariable Long id
    ) {

        return examenRepository.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    /**
     * Modifier un examen.
     * Ne touche volontairement pas à ecole/anneeScolaire.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ExamenDTO> modifierExamen(
            @PathVariable Long id,
            @RequestBody Examen examen
    ) {

        return examenRepository.findById(id)
                .map(existant -> {

                    existant.setLibelle(examen.getLibelle());
                    existant.setDateExamen(examen.getDateExamen());
                    existant.setHeureDebut(examen.getHeureDebut());
                    existant.setHeureFin(examen.getHeureFin());
                    existant.setActif(examen.isActif());

                    Examen sauvegarde = examenRepository.save(existant);

                    return ResponseEntity.ok(toDto(sauvegarde));
                })
                .orElse(ResponseEntity.notFound().build());
    }


    /**
     * Supprimer un examen.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerExamen(
            @PathVariable Long id
    ) {

        if (!examenRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        examenRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }


    // =========================================================
    // RÉPARTITION (inchangé)
    // =========================================================

    @PostMapping("/{examenId}/repartition")
    public ResponseEntity<List<RepartitionExamenDTO>> repartir(
            @PathVariable Long examenId,
            @RequestBody RepartitionExamenRequest request
    ) {
        return ResponseEntity.ok(
                repartitionExamenService.repartir(examenId, request)
        );
    }

    @GetMapping("/{examenId}/repartition")
    public ResponseEntity<List<RepartitionExamenDTO>> getRepartition(
            @PathVariable Long examenId
    ) {
        return ResponseEntity.ok(
                repartitionExamenService.getRepartition(examenId)
        );
    }

    @GetMapping("/{examenId}/repartition/salle/{salleId}")
    public ResponseEntity<List<RepartitionExamenDTO>> getRepartitionSalle(
            @PathVariable Long examenId,
            @PathVariable Long salleId
    ) {
        return ResponseEntity.ok(
                repartitionExamenService.getRepartitionSalle(examenId, salleId)
        );
    }

    @DeleteMapping("/{examenId}/repartition")
    public ResponseEntity<Void> supprimerRepartition(
            @PathVariable Long examenId
    ) {
        repartitionExamenService.supprimerRepartition(examenId);
        return ResponseEntity.noContent().build();
    }


    // =========================================================
    // MAPPING
    // =========================================================

    private ExamenDTO toDto(Examen examen) {

        ExamenDTO dto = new ExamenDTO();

        dto.setId(examen.getId());
        dto.setLibelle(examen.getLibelle());
        dto.setDateExamen(examen.getDateExamen());
        dto.setHeureDebut(examen.getHeureDebut());
        dto.setHeureFin(examen.getHeureFin());
        dto.setActif(examen.isActif());

        if (examen.getEcole() != null) {
            dto.setEcoleId(examen.getEcole().getId());
        }

        if (examen.getAnneeScolaire() != null) {
            dto.setAnneeScolaireId(examen.getAnneeScolaire().getId());
            dto.setAnneeScolaireNom(examen.getAnneeScolaire().getNom());
        }

        return dto;
    }
}
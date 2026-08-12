package com.saas.school.controller;

import com.saas.school.dto.CoefficientMatiereRequest;
import com.saas.school.dto.CoefficientMatiereResponseDTO;
import com.saas.school.entity.CoefficientMatiere;
import com.saas.school.service.CoefficientMatiereService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coefficients")
@RequiredArgsConstructor
@CrossOrigin("*")
public class CoefficientMatiereController {

    private final CoefficientMatiereService coefficientService;


    @PostMapping
    public ResponseEntity<?> creerOuModifier(
            @RequestBody CoefficientMatiereRequest request
    ) {

        try {

            CoefficientMatiere coef =
                    coefficientService.creerOuModifier(request);

            return ResponseEntity.ok(coef);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }



    @GetMapping("/ecole/{ecoleId}")
    public List<CoefficientMatiereResponseDTO> getByEcole(
            @PathVariable Long ecoleId
    ) {

        return coefficientService.getByEcole(ecoleId);
    }



    @GetMapping("/ecole/{ecoleId}/annee/{anneeScolaireId}")
    public List<CoefficientMatiereResponseDTO> getByEcoleEtAnnee(
            @PathVariable Long ecoleId,
            @PathVariable Long anneeScolaireId
    ) {

        return coefficientService.getByEcoleEtAnnee(
                ecoleId,
                anneeScolaireId
        );
    }




    /**
     * Récupérer un programme précis
     * Matière + Niveau + Série + Année
     */
    @GetMapping("/programme")
    public ResponseEntity<?> getProgramme(

            @RequestParam Long ecoleId,

            @RequestParam Long anneeScolaireId,

            @RequestParam Long matiereId,

            @RequestParam Long niveauId,

            @RequestParam(required = false) Long serieId

    ) {

        try {

            return ResponseEntity.ok(
                    coefficientService.getProgramme(
                            ecoleId,
                            matiereId,
                            niveauId,
                            serieId,
                            anneeScolaireId
                    )
            );


        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }

    }





    /**
     * Récupérer tout le programme d'un niveau
     * pour une classe donnée
     */
    @GetMapping("/programme/niveau")
    public ResponseEntity<List<CoefficientMatiereResponseDTO>> getProgrammeParNiveau(

            @RequestParam Long ecoleId,

            @RequestParam Long anneeScolaireId,

            @RequestParam Long niveauId,

            @RequestParam(required = false) Long serieId

    ) {

        return ResponseEntity.ok(
                coefficientService.getProgrammeParNiveau(
                        ecoleId,
                        anneeScolaireId,
                        niveauId,
                        serieId
                )
        );

    }





    /**
     * Retourne uniquement le coefficient
     */
    @GetMapping("/valeur")
    public int getCoefficient(

            @RequestParam Long ecoleId,

            @RequestParam Long anneeScolaireId,

            @RequestParam Long matiereId,

            @RequestParam Long niveauId,

            @RequestParam(required = false) Long serieId

    ) {


        return coefficientService.getCoefficient(
                ecoleId,
                matiereId,
                niveauId,
                serieId,
                anneeScolaireId
        );

    }





    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimer(
            @PathVariable Long id
    ) {

        coefficientService.supprimer(id);

        return ResponseEntity.ok(
                "Coefficient supprimé"
        );
    }

}
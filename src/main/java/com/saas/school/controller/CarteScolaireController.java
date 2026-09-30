package com.saas.school.controller;

import com.saas.school.dto.CarteScolaireDto;
import com.saas.school.entity.Inscription;

import com.saas.school.repository.InscriptionRepository;
import com.saas.school.service.CarteScolairePdfService;

import com.saas.school.service.StatutInscription;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cartes-scolaires")
@RequiredArgsConstructor
public class CarteScolaireController {

    private final CarteScolairePdfService pdfService;
    private final InscriptionRepository inscriptionRepository;

    // =====================================================
    // CARTE INDIVIDUELLE
    // =====================================================

    @GetMapping("/eleve/{inscriptionId}/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> genererCarteEleve(
            @PathVariable Long inscriptionId
    ) {

        Inscription inscription = inscriptionRepository
                .findById(inscriptionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Inscription introuvable : " + inscriptionId
                        )
                );

        if (inscription.getStatut() != StatutInscription.VALIDE) {
            throw new IllegalArgumentException(
                    "L'inscription de cet élève n'est pas validée."
            );
        }

        CarteScolaireDto dto =
                pdfService.construireDto(inscription);

        byte[] pdf =
                pdfService.genererPdfEleve(dto);

        return reponsePdf(
                pdf,
                "carte-scolaire-" + inscriptionId + ".pdf"
        );
    }

    // =====================================================
    // CARTES DE TOUTE UNE CLASSE
    // =====================================================

    @GetMapping("/classe/{classeId}/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> genererCartesClasse(
            @PathVariable Long classeId,
            @RequestParam Long anneeScolaireId
    ) {

        List<Inscription> inscriptions =
                inscriptionRepository.findElevesValidesPourReleve(
                        classeId,
                        anneeScolaireId
                );

        if (inscriptions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Aucun élève validé dans cette classe pour cette année scolaire."
            );
        }

        List<CarteScolaireDto> dtos = inscriptions
                .stream()
                .map(pdfService::construireDto)
                .toList();

        byte[] pdf =
                pdfService.genererPdfClasse(dtos);

        return reponsePdf(
                pdf,
                "cartes-scolaires-classe-" + classeId + ".pdf"
        );
    }

    // =====================================================
    // RÉPONSE PDF
    // =====================================================

    private ResponseEntity<byte[]> reponsePdf(
            byte[] pdf,
            String nomFichier
    ) {

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nomFichier + "\""
                )
                .contentLength(pdf.length)
                .body(pdf);
    }
}
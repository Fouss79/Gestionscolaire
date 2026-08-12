package com.saas.school.service;

import com.saas.school.dto.PaiementRequestDTO;
import com.saas.school.dto.PaiementResponseDTO;
import com.saas.school.entity.*;
import com.saas.school.repository.LigneFraisRepository;
import com.saas.school.repository.PaiementRepository;
import com.saas.school.repository.PaiementScolariteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaiementService {

    private final PaiementRepository paiementRepository;
    private final PaiementScolariteRepository paiementScolariteRepository;
    private final LigneFraisRepository ligneFraisRepository;

    public Paiement creerPaiement(Long ecoleId, PlanAbonnement plan, int duree) {

        double montant = calculerMontant(plan, duree);

        Paiement p = new Paiement();
        p.setEcoleId(ecoleId);
        p.setPlan(plan);
        p.setDuree(duree);
        p.setMontant(montant);
        p.setStatus("PENDING");
        p.setCreatedAt(LocalDateTime.now());

        return paiementRepository.save(p);
    }

    private double calculerMontant(PlanAbonnement plan, int duree) {

        return switch (plan) {
            case BASIC -> 5000 * duree;
            case PRO -> 10000 * duree;
            case PREMIUM -> 20000 * duree;
        };
    }

    @Transactional
    public PaiementResponseDTO enregistrerPaiement(PaiementRequestDTO dto){
        if (dto.getInscriptionId() == null) {
            throw new RuntimeException("L'inscription est obligatoire.");
        }

        if (dto.getCodeTypeFrais() == null || dto.getCodeTypeFrais().isBlank()) {
            throw new RuntimeException("Le type de frais est obligatoire.");
        }

        LigneFrais ligne;

        // Frais mensuels
        if (dto.getMois() != null) {

            ligne = ligneFraisRepository
                    .findByInscriptionIdAndTypeFrais_CodeAndMois(
                            dto.getInscriptionId(),
                            dto.getCodeTypeFrais(),
                            dto.getMois()
                    )
                    .orElseThrow(() -> new RuntimeException(
                            "Aucune ligne de frais '" + dto.getCodeTypeFrais()
                                    + "' pour le mois " + dto.getMois()
                    ));

        }
        // Frais annuels
        else {

            ligne = ligneFraisRepository
                    .findByInscriptionIdAndTypeFrais_CodeAndMoisIsNull(
                            dto.getInscriptionId(),
                            dto.getCodeTypeFrais()
                    )
                    .orElseThrow(() -> new RuntimeException(
                            "Aucune ligne de frais '" + dto.getCodeTypeFrais()
                                    + "' pour cette inscription."
                    ));
        }

        if (ligne.getInscription().getStatut() == StatutInscription.REFUSE) {
            throw new RuntimeException("Impossible d'enregistrer un paiement pour un dossier rejeté.");
        }

        if (dto.getMontant() == null || dto.getMontant() <= 0) {
            throw new RuntimeException("Le montant doit être supérieur à zéro.");
        }

        double montantPayeActuel = ligne.getMontantPaye() == null ? 0.0 : ligne.getMontantPaye();
        double montantTotal = ligne.getMontantTotal() == null ? 0.0 : ligne.getMontantTotal();
        double resteActuel = montantTotal - montantPayeActuel;

        if (dto.getMontant() > resteActuel) {
            throw new RuntimeException(
                    "Le montant (" + dto.getMontant()
                            + ") dépasse le reste à payer (" + resteActuel + ")."
            );
        }

        PaiementScolarite paiement = new PaiementScolarite();
        paiement.setLigneFrais(ligne);
        paiement.setMontant(dto.getMontant());
        paiement.setModePaiement(dto.getModePaiement());

        if ("CASH".equalsIgnoreCase(dto.getModePaiement())) {
            paiement.setReference(genererReferenceCash());
        } else {
            paiement.setReference(dto.getReference());
        }

        paiement.setDatePaiement(LocalDateTime.now());

        double nouveauMontantPaye = montantPayeActuel + dto.getMontant();

        ligne.setMontantPaye(nouveauMontantPaye);
        ligne.setResteAPayer(montantTotal - nouveauMontantPaye);

        if (nouveauMontantPaye >= montantTotal) {
            ligne.setStatutPaiement(StatutPaiement.PAYE);
        } else {
            ligne.setStatutPaiement(StatutPaiement.PARTIEL);
        }

        ligneFraisRepository.save(ligne);

        PaiementScolarite saved = paiementScolariteRepository.save(paiement);

        return mapToDto(saved);
    }

    private String genererReferenceCash() {
        return "REC-" + System.currentTimeMillis();
    }

    public void validerPaiement(Long paiementId) {

        Paiement p = paiementRepository.findById(paiementId)
                .orElseThrow();

        p.setStatus("SUCCESS");

        paiementRepository.save(p);
    }
    public List<PaiementResponseDTO> getByInscription(Long inscriptionId) {
        return paiementScolariteRepository
                .findByLigneFrais_Inscription_IdOrderByDatePaiementDesc(inscriptionId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<PaiementResponseDTO> getByEcole(Long ecoleId) {
        return paiementScolariteRepository
                .findByLigneFrais_Inscription_Ecole_IdOrderByDatePaiementDesc(ecoleId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private PaiementResponseDTO mapToDto(PaiementScolarite p) {

        LigneFrais ligne = p.getLigneFrais();
        Inscription inscription = ligne.getInscription();
        Eleve eleve = inscription.getEleve();

        PaiementResponseDTO dto = new PaiementResponseDTO();
        dto.setId(p.getId());
        dto.setInscriptionId(inscription.getId());
        dto.setEleveNom(eleve.getNom());
        dto.setElevePrenom(eleve.getPrenom());
        dto.setTypeFraisCode(ligne.getTypeFrais().getCode());
        dto.setTypeFraisLibelle(ligne.getTypeFrais().getLibelle());
        dto.setMois(ligne.getMois());
        dto.setAnnee(ligne.getAnnee());
        dto.setMontant(p.getMontant());
        dto.setModePaiement(p.getModePaiement());
        dto.setReference(p.getReference());
        dto.setDatePaiement(p.getDatePaiement());
        if (inscription.getClasse() != null) {
            dto.setClasseNom(
                    inscription.getClasse().getNomComplet()
            );
        }

        if (inscription.getAnneeScolaire() != null) {
            dto.setAnneeScolaireNom(
                    inscription.getAnneeScolaire().getNom()
            );
        }

        return dto;
    }

}
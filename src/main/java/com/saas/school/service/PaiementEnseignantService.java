package com.saas.school.service;

import com.saas.school.dto.EmargementResumeDTO;
import com.saas.school.dto.PaiementEnseignantDTO;
import com.saas.school.entity.Enseignant;
import com.saas.school.entity.PaiementEnseignant;
import com.saas.school.repository.EnseignantRepository;
import com.saas.school.repository.PaiementEnseignantRepository;
import com.saas.school.repository.PaiementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaiementEnseignantService {

    private final PaiementEnseignantRepository paiementRepo;
    private final EnseignantRepository enseignantRepo;
    private final EmargementService emargementService;
    private final OperationComptableService operationComptableService;

    // ================= PREVISUALISATION (sans sauvegarde) =================
    public List<PaiementEnseignantDTO> previsualiserTous(LocalDate debut, LocalDate fin, Long anneeId) {
        List<EmargementResumeDTO> resumes = emargementService.getResumeTousEnseignants(debut, fin, anneeId);

        return resumes.stream()
                .map(r -> {
                    Enseignant ens = enseignantRepo.findById(r.getEnseignantId())
                            .orElseThrow(() -> new RuntimeException("Enseignant introuvable"));

                    double taux = ens.getTauxHoraire() != null ? ens.getTauxHoraire() : 0.0;
                    double montant = r.getTotalHeuresEmargees() * taux;

                    return PaiementEnseignantDTO.builder()
                            .enseignantId(r.getEnseignantId())
                            .enseignantNom(r.getEnseignantNom())
                            .enseignantPrenom(r.getEnseignantPrenom())
                            .periodeDebut(debut)
                            .periodeFin(fin)
                            .totalHeures(r.getTotalHeuresEmargees())
                            .tauxHoraire(taux)
                            .montant(montant)
                            .statut("NON_GENERE")
                            .build();
                })
                .toList();
    }

    // ================= GENERATION (persiste, sans doublon) =================
    @Transactional
    public List<PaiementEnseignantDTO> genererPaiements(LocalDate debut, LocalDate fin, Long anneeId) {
        List<EmargementResumeDTO> resumes = emargementService.getResumeTousEnseignants(debut, fin, anneeId);

        List<PaiementEnseignantDTO> resultats = new ArrayList<>();

        for (EmargementResumeDTO r : resumes) {

            boolean dejaGenere = paiementRepo.existsByEnseignant_IdAndPeriodeDebutAndPeriodeFin(
                    r.getEnseignantId(), debut, fin);

            if (dejaGenere) continue; // évite de payer deux fois la même période

            Enseignant ens = enseignantRepo.findById(r.getEnseignantId())
                    .orElseThrow(() -> new RuntimeException("Enseignant introuvable"));

            double taux = ens.getTauxHoraire() != null ? ens.getTauxHoraire() : 0.0;
            double montant = r.getTotalHeuresEmargees() * taux;

            PaiementEnseignant paiement = PaiementEnseignant.builder()
                    .enseignant(ens)
                    .periodeDebut(debut)
                    .periodeFin(fin)
                    .totalHeures(r.getTotalHeuresEmargees())
                    .tauxHoraire(taux)
                    .montant(montant)
                    .statut(PaiementEnseignant.StatutPaiement.EN_ATTENTE)
                    .anneeScolaireId(anneeId)
                    .build();

            paiementRepo.save(paiement);
            resultats.add(toDTO(paiement));
        }

        return resultats;
    }

    // ================= MARQUER PAYE =================
    @Transactional
    public PaiementEnseignantDTO marquerPaye(Long paiementId) {

        PaiementEnseignant paiement = paiementRepo.findById(paiementId)
                .orElseThrow(() -> new RuntimeException("Paiement enseignant introuvable"));

        // Empêche de créer deux dépenses pour le même paiement
        if (paiement.getStatut() == PaiementEnseignant.StatutPaiement.PAYE) {
            return toDTO(paiement);
        }

        paiement.setStatut(PaiementEnseignant.StatutPaiement.PAYE);
        paiement.setDatePaiement(LocalDate.now());

        PaiementEnseignant paiementSauvegarde = paiementRepo.save(paiement);

        // Création de l'opération comptable
        operationComptableService.creerDepenseDepuisPaiementEnseignant(
                paiementSauvegarde
        );

        return toDTO(paiementSauvegarde);
    }

    // ================= LISTER =================
    public List<PaiementEnseignantDTO> listerPaiements(Long anneeId) {
        return paiementRepo.findByAnneeScolaireId(anneeId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private PaiementEnseignantDTO toDTO(PaiementEnseignant p) {
        return PaiementEnseignantDTO.builder()
                .id(p.getId())
                .enseignantId(p.getEnseignant().getId())
                .enseignantNom(p.getEnseignant().getNom())
                .enseignantPrenom(p.getEnseignant().getPrenom())
                .periodeDebut(p.getPeriodeDebut())
                .periodeFin(p.getPeriodeFin())
                .totalHeures(p.getTotalHeures())
                .tauxHoraire(p.getTauxHoraire())
                .montant(p.getMontant())
                .statut(p.getStatut().name())
                .datePaiement(p.getDatePaiement())
                .build();
    }
}
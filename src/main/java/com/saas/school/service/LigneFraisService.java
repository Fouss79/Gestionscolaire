package com.saas.school.service;

import com.saas.school.dto.LigneFraisDTO;
import com.saas.school.entity.*;
import com.saas.school.repository.LigneFraisRepository;
import com.saas.school.repository.TarifRepository;
import com.saas.school.repository.TypeFraisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LigneFraisService {

    private static final double MONTANT_PAR_DEFAUT = 2000.0;

    private final LigneFraisRepository ligneFraisRepository;
    private final TypeFraisRepository typeFraisRepository;
    private final TarifRepository tarifRepository;

    /**
     * Génère une ligne de frais pour chaque type de frais de l'école.
     * Si aucun tarif n'est configuré pour le niveau/l'année, un montant par
     * défaut est appliqué et la ligne est marquée "estimatif" pour alerte.
     */
    public void genererLignesFrais(Inscription inscription) {

        Long ecoleId = inscription.getEcole().getId();
        Long niveauId = inscription.getClasse().getNiveau().getId();
        AnneeScolaire anneeScolaire = inscription.getAnneeScolaire();
        Long anneeId = anneeScolaire.getId();

        List<TypeFrais> typesFrais = typeFraisRepository.findByEcoleId(ecoleId);

        for (TypeFrais type : typesFrais) {

            Optional<Tarif> tarifTrouve = tarifRepository
                    .findByNiveauIdAndAnneeScolaireIdAndTypeFrais_Code(niveauId, anneeId, type.getCode());

            double montant = tarifTrouve.map(Tarif::getMontant).orElse(MONTANT_PAR_DEFAUT);
            boolean estimatif = tarifTrouve.isEmpty();

            if (type.getFrequence() == FrequenceFrais.MENSUEL) {
                genererLignesMensuelles(inscription, type, montant, anneeScolaire, estimatif);
            } else {
                genererLigneAnnuelle(inscription, type, montant, estimatif);
            }
        }
    }

    /**
     * FRAIS UNIQUE / ANNUEL — une seule ligne, sans mois.
     */
    private void genererLigneAnnuelle(Inscription inscription, TypeFrais type, Double montant, boolean estimatif) {

        boolean existe = ligneFraisRepository
                .existsByInscriptionIdAndTypeFraisIdAndMoisIsNull(inscription.getId(), type.getId());

        if (existe) {
            return;
        }

        LigneFrais ligne = new LigneFrais();
        ligne.setInscription(inscription);
        ligne.setTypeFrais(type);
        ligne.setMontantTotal(montant);
        ligne.setMontantPaye(0.0);
        ligne.setResteAPayer(montant);
        ligne.setStatutPaiement(montant > 0 ? StatutPaiement.NON_PAYE : StatutPaiement.PAYE);
        ligne.setEstimatif(estimatif);

        ligneFraisRepository.save(ligne);
    }

    /**
     * FRAIS MENSUEL — ex: SCOLARITE de septembre à juin.
     * Répartit le montant total sur 10 mois, avec la vraie année calendaire
     * (septembre-décembre → année de début, janvier-juin → année de fin).
     */

    private void genererLignesMensuelles(
            Inscription inscription,
            TypeFrais type,
            Double montantAnnuel,
            AnneeScolaire anneeScolaire,
            boolean estimatif
    ) {

        YearMonth debut = YearMonth.from(anneeScolaire.getDateDebut());
        YearMonth fin = YearMonth.from(anneeScolaire.getDateFin());

        long nombreMois = ChronoUnit.MONTHS.between(debut, fin) + 1;

        double montantMensuel = montantAnnuel / nombreMois;

        YearMonth courant = debut;

        while (!courant.isAfter(fin)) {

            creerLigneMensuelle(
                    inscription,
                    type,
                    montantMensuel,
                    courant.getMonthValue(),
                    courant.getYear(),
                    estimatif
            );

            courant = courant.plusMonths(1);
        }
    }
    private void creerLigneMensuelle(
            Inscription inscription,
            TypeFrais type,
            Double montant,
            Integer mois,
            Integer annee,
            boolean estimatif
    ) {

        boolean existe = ligneFraisRepository
                .existsByInscriptionIdAndTypeFraisIdAndMoisAndAnnee(
                        inscription.getId(),
                        type.getId(),
                        mois,
                        annee
                );

        if (existe) {
            return;
        }

        LigneFrais ligne = new LigneFrais();
        ligne.setInscription(inscription);
        ligne.setTypeFrais(type);
        ligne.setMois(mois);
        ligne.setAnnee(annee);
        ligne.setMontantTotal(montant);
        ligne.setMontantPaye(0.0);
        ligne.setResteAPayer(montant);
        ligne.setStatutPaiement(montant > 0 ? StatutPaiement.NON_PAYE : StatutPaiement.PAYE);
        ligne.setEstimatif(estimatif);

        ligneFraisRepository.save(ligne);
    }

    // =========================
    // 📥 LECTURE
    // =========================

    public List<LigneFrais> getByInscription(Long inscriptionId) {
        return ligneFraisRepository.findByInscriptionId(inscriptionId);
    }

    public List<LigneFraisDTO> getByEcole(Long ecoleId) {

        List<LigneFrais> lignes =
                ligneFraisRepository
                        .findByInscription_Ecole_IdAndInscription_AnneeScolaire_ActiveTrue(ecoleId);

        lignes.forEach(l ->
                System.out.println(
                        "ID=" + l.getId()
                                + " TYPE=" + l.getTypeFrais().getCode()
                                + " MOIS=" + l.getMois()
                                + " ANNEE=" + l.getAnnee()
                )
        );

        return lignes.stream()
                .map(this::mapToDto)
                .toList();
    }
    public LigneFrais getById(Long id) {
        return ligneFraisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ligne de frais introuvable"));
    }

    public LigneFrais getByInscriptionAndType(Long inscriptionId, String codeTypeFrais) {
        return ligneFraisRepository.findByInscriptionIdAndTypeFrais_Code(inscriptionId, codeTypeFrais)
                .orElseThrow(() -> new RuntimeException("Ligne de frais introuvable"));
    }

    public void supprimer(Long id) {
        if (!ligneFraisRepository.existsById(id)) {
            throw new RuntimeException("Ligne de frais introuvable");
        }
        ligneFraisRepository.deleteById(id);
    }

    // =========================
    // 🔁 MAPPING
    // =========================

    private LigneFraisDTO mapToDto(LigneFrais ligne) {

        LigneFraisDTO dto = new LigneFraisDTO();

        dto.setId(ligne.getId());
        dto.setInscriptionId(ligne.getInscription().getId());
        dto.setEleveNom(ligne.getInscription().getEleve().getNom());
        dto.setElevePrenom(ligne.getInscription().getEleve().getPrenom());
        dto.setClasseNom(ligne.getInscription().getClasse().getNomComplet());
        dto.setTypeFraisCode(ligne.getTypeFrais().getCode());
        dto.setTypeFraisLibelle(ligne.getTypeFrais().getLibelle());
        dto.setMois(ligne.getMois());
        dto.setAnnee(ligne.getAnnee());
        dto.setMontantTotal(ligne.getMontantTotal());
        dto.setMontantPaye(ligne.getMontantPaye());
        dto.setResteAPayer(ligne.getResteAPayer());
        dto.setStatutPaiement(ligne.getStatutPaiement().name());
        dto.setEstimatif(ligne.isEstimatif());

        return dto;
    }
    /**
     * Recalcule toutes les lignes de frais "estimatives" (tarif non défini au moment
     * de leur création) pour un niveau/année/type de frais donné, une fois qu'un vrai
     * tarif vient d'être configuré par l'admin.
     */
    @org.springframework.transaction.annotation.Transactional
    public int recalculerLignesEstimatives(Long niveauId, Long anneeScolaireId, String codeTypeFrais, Double nouveauMontantAnnuel) {

        List<LigneFrais> lignesEstimatives = ligneFraisRepository
                .findByInscription_Classe_Niveau_IdAndInscription_AnneeScolaire_IdAndTypeFrais_CodeAndEstimatifTrue(
                        niveauId, anneeScolaireId, codeTypeFrais
                );

        if (lignesEstimatives.isEmpty()) {
            return 0;
        }

        boolean estMensuel = lignesEstimatives.stream().anyMatch(l -> l.getMois() != null);
        double nouveauMontantParLigne = nouveauMontantAnnuel;

        if (estMensuel) {

            AnneeScolaire annee = lignesEstimatives.get(0)
                    .getInscription()
                    .getAnneeScolaire();

            YearMonth debut = YearMonth.from(annee.getDateDebut());
            YearMonth fin = YearMonth.from(annee.getDateFin());

            long nbMois = ChronoUnit.MONTHS.between(debut, fin) + 1;

            nouveauMontantParLigne = nouveauMontantAnnuel / nbMois;
        }
        for (LigneFrais ligne : lignesEstimatives) {

            double montantDejaPaye = ligne.getMontantPaye() != null ? ligne.getMontantPaye() : 0.0;

            // 🔥 On recalcule TOUJOURS le total réel, même si un paiement a déjà eu lieu
            double nouveauReste = Math.max(0.0, nouveauMontantParLigne - montantDejaPaye);

            ligne.setMontantTotal(nouveauMontantParLigne);
            ligne.setResteAPayer(nouveauReste);
            ligne.setEstimatif(false);

            if (nouveauReste <= 0) {
                ligne.setStatutPaiement(StatutPaiement.PAYE);
            } else if (montantDejaPaye > 0) {
                ligne.setStatutPaiement(StatutPaiement.PARTIEL);
            } else {
                ligne.setStatutPaiement(StatutPaiement.NON_PAYE);
            }

            ligneFraisRepository.save(ligne);
        }

        return lignesEstimatives.size();
    }

}
package com.saas.school.service;

import com.saas.school.dto.LigneFraisDTO;
import com.saas.school.entity.*;
import com.saas.school.repository.InscriptionRepository;
import com.saas.school.repository.LigneFraisRepository;
import com.saas.school.repository.TarifRepository;
import com.saas.school.repository.TypeFraisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LigneFraisService {

    private static final double MONTANT_PAR_DEFAUT = 2000.0;

    private final LigneFraisRepository ligneFraisRepository;
    private final TypeFraisRepository typeFraisRepository;
    private final TarifRepository tarifRepository;
    private final InscriptionRepository inscriptionRepository;

    /**
     * Génère une ligne de frais pour chaque type de frais de l'école.
     * Si aucun tarif n'est configuré pour le niveau/l'année, un montant par
     * défaut est appliqué et la ligne est marquée "estimatif" pour alerte.
     *
     * Il n'y a désormais QU'UNE SEULE ligne par (inscription, typeFrais),
     * quelle que soit la fréquence :
     *   - ANNUEL : montant annuel total, payable en tranches mois par mois
     *              (le détail par mois est calculé dynamiquement à partir
     *              des paiements, voir PaiementService.getSuiviMensuel).
     *   - UNIQUE : montant payable en une seule fois.
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

            genererLigneFrais(inscription, type, montant, estimatif);
        }
    }

    /**
     * Crée l'unique ligne de frais pour ce type, si elle n'existe pas déjà.
     * Valable pour ANNUEL comme pour UNIQUE.
     */
    private void genererLigneFrais(Inscription inscription, TypeFrais type, Double montant, boolean estimatif) {

        boolean existe = ligneFraisRepository
                .existsByInscriptionIdAndTypeFraisId(inscription.getId(), type.getId());

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
     * RATTRAPAGE — applique un type de frais aux inscriptions déjà
     * existantes qui n'en ont pas encore la ligne.
     *
     * Utile quand un nouveau TypeFrais est créé APRÈS que des élèves
     * soient déjà inscrits : genererLignesFrais() n'est appelée qu'à la
     * création de l'inscription, donc les élèves existants n'obtiennent
     * jamais automatiquement les types créés après coup. Cette méthode
     * comble cet écart, à appeler juste après la création d'un TypeFrais
     * (ou via une action manuelle "Appliquer aux élèves existants").
     *
     * Ne recrée jamais une ligne déjà existante (idempotent).
     */
    @org.springframework.transaction.annotation.Transactional
    public int appliquerTypeFraisAuxInscriptionsExistantes(Long typeFraisId) {

        TypeFrais type = typeFraisRepository.findById(typeFraisId)
                .orElseThrow(() -> new RuntimeException("Type de frais introuvable"));

        Long ecoleId = type.getEcole().getId();

        // Toutes les inscriptions actives de l'école (années scolaires actives)
        List<Inscription> inscriptions =
                inscriptionRepository.findByEcole_IdAndAnneeScolaire_ActiveTrue(ecoleId);

        int nbLignesCreees = 0;

        for (Inscription inscription : inscriptions) {

            boolean existe = ligneFraisRepository
                    .existsByInscriptionIdAndTypeFraisId(inscription.getId(), type.getId());

            if (existe) {
                continue;
            }

            Long niveauId = inscription.getClasse().getNiveau().getId();
            Long anneeId = inscription.getAnneeScolaire().getId();

            Optional<Tarif> tarifTrouve = tarifRepository
                    .findByNiveauIdAndAnneeScolaireIdAndTypeFrais_Code(niveauId, anneeId, type.getCode());

            double montant = tarifTrouve.map(Tarif::getMontant).orElse(MONTANT_PAR_DEFAUT);
            boolean estimatif = tarifTrouve.isEmpty();

            genererLigneFrais(inscription, type, montant, estimatif);
            nbLignesCreees++;
        }

        return nbLignesCreees;
    }

    // =========================
    // 📥 LECTURE (entités brutes — usage interne)
    // =========================

    public List<LigneFrais> getByInscription(Long inscriptionId) {
        return ligneFraisRepository.findByInscriptionId(inscriptionId);
    }

    public LigneFrais getById(Long id) {
        return ligneFraisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ligne de frais introuvable"));
    }

    public LigneFrais getByInscriptionAndType(Long inscriptionId, String codeTypeFrais) {
        return ligneFraisRepository.findByInscriptionIdAndTypeFrais_Code(inscriptionId, codeTypeFrais)
                .orElseThrow(() -> new RuntimeException("Ligne de frais introuvable"));
    }

    // =========================
    // 📥 LECTURE (DTO — exposé au contrôleur)
    // =========================

    public List<LigneFraisDTO> getByEcole(Long ecoleId) {

        List<LigneFrais> lignes =
                ligneFraisRepository
                        .findByInscription_Ecole_IdAndInscription_AnneeScolaire_ActiveTrue(ecoleId);

        return lignes.stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<LigneFraisDTO> getByInscriptionDTO(Long inscriptionId) {
        return getByInscription(inscriptionId).stream()
                .map(this::mapToDto)
                .toList();
    }

    public LigneFraisDTO getByIdDTO(Long id) {
        return mapToDto(getById(id));
    }

    public LigneFraisDTO getByInscriptionAndTypeDTO(Long inscriptionId, String codeTypeFrais) {
        return mapToDto(getByInscriptionAndType(inscriptionId, codeTypeFrais));
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
        dto.setTypeFraisFrequence(ligne.getTypeFrais().getFrequence().name());
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
     *
     * Comme il n'existe désormais qu'une seule ligne par (inscription, typeFrais),
     * le nouveau montant s'applique tel quel — qu'il s'agisse d'un type ANNUEL
     * (montant annuel total, réparti en tranches mensuelles au moment du paiement)
     * ou UNIQUE (montant à payer en une fois).
     */
    @org.springframework.transaction.annotation.Transactional
    public int recalculerLignesEstimatives(Long niveauId, Long anneeScolaireId, String codeTypeFrais, Double nouveauMontant) {

        // 🔥 On recalcule TOUTES les lignes du niveau/année/type, estimatives ou non
        List<LigneFrais> lignesAMettreAJour = ligneFraisRepository
                .findByInscription_Classe_Niveau_IdAndInscription_AnneeScolaire_IdAndTypeFrais_Code(
                        niveauId, anneeScolaireId, codeTypeFrais
                );

        if (lignesAMettreAJour.isEmpty()) {
            return 0;
        }

        for (LigneFrais ligne : lignesAMettreAJour) {

            double montantDejaPaye = ligne.getMontantPaye() != null ? ligne.getMontantPaye() : 0.0;
            double nouveauReste = Math.max(0.0, nouveauMontant - montantDejaPaye);

            ligne.setMontantTotal(nouveauMontant);
            ligne.setResteAPayer(nouveauReste);
            ligne.setEstimatif(false); // le tarif est désormais réel/à jour

            if (nouveauReste <= 0) {
                ligne.setStatutPaiement(StatutPaiement.PAYE);
            } else if (montantDejaPaye > 0) {
                ligne.setStatutPaiement(StatutPaiement.PARTIEL);
            } else {
                ligne.setStatutPaiement(StatutPaiement.NON_PAYE);
            }

            ligneFraisRepository.save(ligne);
        }

        return lignesAMettreAJour.size();
    }
}
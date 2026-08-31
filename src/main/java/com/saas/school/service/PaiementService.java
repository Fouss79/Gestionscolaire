package com.saas.school.service;

import com.saas.school.dto.MoisPaiementDTO;
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
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public PaiementResponseDTO enregistrerPaiement(PaiementRequestDTO dto) {

        if (dto.getInscriptionId() == null) {
            throw new RuntimeException("L'inscription est obligatoire.");
        }

        if (dto.getCodeTypeFrais() == null || dto.getCodeTypeFrais().isBlank()) {
            throw new RuntimeException("Le type de frais est obligatoire.");
        }

        if (dto.getMontant() == null || dto.getMontant() <= 0) {
            throw new RuntimeException("Le montant doit être supérieur à zéro.");
        }

        /*
         * UNE SEULE LIGNE DE FRAIS PAR TYPE ET PAR INSCRIPTION,
         * que le type soit ANNUEL (payable par tranches mensuelles) ou
         * UNIQUE (payable en une fois).
         *
         * Exemple :
         *
         * Inscription 15
         * Scolarité (ANNUEL)
         * 300 000 FCFA
         *
         * reste à payer : 180 000 FCFA
         *
         * Tous les paiements seront rattachés à cette même ligne.
         */
        LigneFrais ligne = ligneFraisRepository
                .findByInscriptionIdAndTypeFrais_Code(
                        dto.getInscriptionId(),
                        dto.getCodeTypeFrais()
                )
                .orElseThrow(() -> new RuntimeException(
                        "Aucune ligne de frais '" +
                                dto.getCodeTypeFrais() +
                                "' pour cette inscription."
                ));

        // Vérification inscription rejetée
        if (ligne.getInscription().getStatut() == StatutInscription.REFUSE) {
            throw new RuntimeException(
                    "Impossible d'enregistrer un paiement pour un dossier rejeté."
            );
        }

        /*
         * Pour un type ANNUEL (payable mois par mois), le mois et l'année
         * sont obligatoires afin de savoir à quelle tranche rattacher le
         * paiement. Pour un type UNIQUE, ils ne sont pas utilisés.
         */
        if (ligne.getTypeFrais().getFrequence() == FrequenceFrais.ANNUEL) {
            if (dto.getMois() == null || dto.getAnnee() == null) {
                throw new RuntimeException(
                        "Le mois et l'année sont obligatoires pour ce type de frais."
                );
            }
        }

        double montantPayeActuel =
                ligne.getMontantPaye() == null
                        ? 0.0
                        : ligne.getMontantPaye();

        double montantTotal =
                ligne.getMontantTotal() == null
                        ? 0.0
                        : ligne.getMontantTotal();

        double resteActuel = montantTotal - montantPayeActuel;

        /*
         * Impossible de payer plus que le reste global de la ligne.
         */
        if (dto.getMontant() > resteActuel) {
            throw new RuntimeException(
                    "Le montant (" +
                            dto.getMontant() +
                            ") dépasse le reste à payer (" +
                            resteActuel +
                            ")."
            );
        }

        /*
         * Création du paiement
         */
        PaiementScolarite paiement = new PaiementScolarite();

        paiement.setLigneFrais(ligne);
        paiement.setMontant(dto.getMontant());
        paiement.setModePaiement(dto.getModePaiement());

        /*
         * Le mois/année du paiement (utile uniquement pour les types ANNUEL,
         * payables par tranches mensuelles) est enregistré SUR LE PAIEMENT,
         * et non sur LigneFrais — LigneFrais ne porte plus de mois.
         */
        paiement.setMois(dto.getMois());
        paiement.setAnnee(dto.getAnnee());

        if ("CASH".equalsIgnoreCase(dto.getModePaiement())) {
            paiement.setReference(genererReferenceCash());
        } else {

            if (dto.getReference() == null ||
                    dto.getReference().isBlank()) {

                throw new RuntimeException(
                        "La référence est obligatoire pour ce mode de paiement."
                );
            }

            paiement.setReference(dto.getReference().trim());
        }

        paiement.setDatePaiement(LocalDateTime.now());

        /*
         * Mise à jour de la ligne de frais
         */
        double nouveauMontantPaye =
                montantPayeActuel + dto.getMontant();

        double nouveauReste =
                montantTotal - nouveauMontantPaye;

        ligne.setMontantPaye(nouveauMontantPaye);
        ligne.setResteAPayer(nouveauReste);

        if (nouveauReste <= 0) {

            ligne.setResteAPayer(0.0);
            ligne.setStatutPaiement(StatutPaiement.PAYE);

        } else if (nouveauMontantPaye > 0) {

            ligne.setStatutPaiement(StatutPaiement.PARTIEL);

        } else {

            ligne.setStatutPaiement(StatutPaiement.NON_PAYE);
        }

        ligneFraisRepository.save(ligne);

        PaiementScolarite saved =
                paiementScolariteRepository.save(paiement);

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

    /**
     * Calcule, pour une ligne de frais de type ANNUEL, le statut mois par
     * mois (montant dû, payé, reste) en répartissant le montant annuel
     * également sur les mois de l'année scolaire, et en sommant les
     * paiements déjà enregistrés pour chaque mois.
     *
     * Ne s'applique pas aux types UNIQUE (payés en une seule fois, sans
     * notion de mois).
     */
    public List<MoisPaiementDTO> getSuiviMensuel(Long ligneId) {

        LigneFrais ligne = ligneFraisRepository.findById(ligneId)
                .orElseThrow(() -> new RuntimeException("Ligne de frais introuvable"));

        if (ligne.getTypeFrais().getFrequence() != FrequenceFrais.ANNUEL) {
            throw new RuntimeException("Ce type de frais n'est pas payable par tranches mensuelles.");
        }

        AnneeScolaire annee = ligne.getInscription().getAnneeScolaire();

        YearMonth debut = YearMonth.from(annee.getDateDebut());
        YearMonth fin = YearMonth.from(annee.getDateFin());

        long nbMois = ChronoUnit.MONTHS.between(debut, fin) + 1;
        double montantTotal = ligne.getMontantTotal() != null ? ligne.getMontantTotal() : 0.0;
        double montantDuParMois = montantTotal / nbMois;

        List<PaiementScolarite> paiements = paiementScolariteRepository.findByLigneFraisId(ligneId);

        Map<String, Double> payeParMois = paiements.stream()
                .filter(p -> p.getMois() != null && p.getAnnee() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getMois() + "-" + p.getAnnee(),
                        Collectors.summingDouble(PaiementScolarite::getMontant)
                ));

        List<MoisPaiementDTO> resultat = new ArrayList<>();
        YearMonth courant = debut;

        while (!courant.isAfter(fin)) {

            String cle = courant.getMonthValue() + "-" + courant.getYear();
            double paye = payeParMois.getOrDefault(cle, 0.0);
            double reste = Math.max(0.0, montantDuParMois - paye);

            MoisPaiementDTO moisDto = new MoisPaiementDTO();
            moisDto.setMois(courant.getMonthValue());
            moisDto.setAnnee(courant.getYear());
            moisDto.setMontantDu(montantDuParMois);
            moisDto.setMontantPaye(paye);
            moisDto.setResteAPayer(reste);
            moisDto.setStatut(reste <= 0 ? "PAYE" : (paye > 0 ? "PARTIEL" : "NON_PAYE"));

            resultat.add(moisDto);
            courant = courant.plusMonths(1);
        }

        return resultat;
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

        dto.setTypeFraisCode(
                ligne.getTypeFrais().getCode()
        );

        dto.setTypeFraisLibelle(
                ligne.getTypeFrais().getLibelle()
        );

        // Le mois appartient au paiement (utile pour les types ANNUEL)
        dto.setMois(p.getMois());
        dto.setAnnee(p.getAnnee());

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
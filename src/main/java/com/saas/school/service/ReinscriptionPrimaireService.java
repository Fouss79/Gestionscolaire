
package com.saas.school.service;

import com.saas.school.dto.*;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReinscriptionPrimaireService {

    private final InscriptionRepository inscriptionRepository;
    private final AnneeScolaireRepository anneeRepository;
    private final ClasseRepository classeRepository;
    private final ResultatFinAnneePrimaireService resultatService;
    private final LigneFraisService ligneFraisService;

    @Transactional(readOnly = true)
    public List<ReinscriptionPrimaireDto> lister(Long ecoleId) {

        AnneeScolaire active = anneeRepository
                .findByEcoleIdAndActiveTrue(ecoleId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Aucune année scolaire active"));

        AnneeScolaire precedente = anneeRepository
                .findTopByEcoleIdAndDateFinBeforeOrderByDateFinDesc(
                        ecoleId,
                        active.getDateDebut()
                )
                .orElse(null);

        if (precedente == null) {
            return List.of();
        }


        return inscriptionRepository
                .findByEcoleIdAndAnneeScolaireId(
                        ecoleId,
                        precedente.getId()
                )
                .stream()
                .filter(i ->
                        i.getStatut() == StatutInscription.VALIDE
                                && i.getClasse() != null
                                && i.getClasse().getNiveau() != null
                                && i.getClasse().getNiveau().getCycle() != null
                                && estCyclePrimaire(
                                i.getClasse().getNiveau().getCycle()
                        )
                )
                .map(i -> construireDto(i, active))
                .toList();
    }

    private ReinscriptionPrimaireDto construireDto(
            Inscription ancienne,
            AnneeScolaire active
    ) {
        ResultatFinAnneeDto resultat =
                resultatService.construirePourEleve(
                        ancienne.getId(),
                        ancienne.getAnneeScolaire().getId()
                );

        Inscription nouvelle = inscriptionRepository
                .findByEleveIdAndAnneeScolaireId(
                        ancienne.getEleve().getId(),
                        active.getId()
                )
                .orElse(null);
        System.out.println("=== REINSCRIPTION PRIMAIRE ===");
        System.out.println("Inscription : " + ancienne.getId());
        System.out.println("Année : "
                + ancienne.getAnneeScolaire().getId());
        System.out.println("Moyenne : "
                + resultat.getMoyenneAnnuelle());

        return new ReinscriptionPrimaireDto(
                ancienne.getId(),
                ancienne.getEleve().getId(),
                resultat.getNomEtPrenom(),
                resultat.getMatricule(),
                ancienne.getClasse().getNomComplet(),
                resultat.getMoyenneAnnuelle(),
                resultat.getRangDansClasse(),
                resultat.getDecision(),
                nouvelle == null
                        ? "NON_REINSCRIT"
                        : "REINSCRIT",
                nouvelle == null ? null : nouvelle.getId(),
                nouvelle == null
                        ? null
                        : nouvelle.getClasse().getId(),
                nouvelle == null
                        ? null
                        : nouvelle.getClasse().getNomComplet()


        );


    }

    @Transactional
    public Inscription reinscrire(
            ReinscriptionPrimaireRequest request
    ) {
        if (request == null
                || request.ancienneInscriptionId() == null
                || request.nouvelleClasseId() == null) {
            throw new IllegalArgumentException(
                    "Ancienne inscription et nouvelle classe obligatoires");
        }

        Inscription ancienne = inscriptionRepository
                .findById(request.ancienneInscriptionId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Ancienne inscription introuvable"));

        if (ancienne.getStatut() != StatutInscription.VALIDE) {
            throw new IllegalArgumentException(
                    "L'ancienne inscription doit être validée");
        }

        Long ecoleId = ancienne.getEcole().getId();

        AnneeScolaire active = anneeRepository
                .findByEcoleIdAndActiveTrue(ecoleId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Aucune année scolaire active"));

        if (!ancienne.getAnneeScolaire()
                .getDateFin()
                .isBefore(active.getDateDebut())) {
            throw new IllegalArgumentException(
                    "L'ancienne inscription doit appartenir " +
                            "à une année scolaire antérieure");
        }

        boolean existe = inscriptionRepository
                .existsByEleveIdAndAnneeScolaireId(
                        ancienne.getEleve().getId(),
                        active.getId()
                );

        if (existe) {
            throw new IllegalArgumentException(
                    "Cet élève possède déjà une inscription " +
                            "pour l'année active");
        }

        Classe nouvelleClasse = classeRepository
                .findById(request.nouvelleClasseId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Nouvelle classe introuvable"));

        if (!Objects.equals(
                nouvelleClasse.getEcole().getId(),
                ecoleId
        )) {
            throw new IllegalArgumentException(
                    "La nouvelle classe appartient à une autre école");
        }

        // À compléter avec la structure exacte de Cycle/Niveau :
        // vérifier que l'ancienne et la nouvelle classe
        // appartiennent bien au primaire.

        Inscription nouvelle = new Inscription();
        nouvelle.setEleve(ancienne.getEleve());
        nouvelle.setEcole(ancienne.getEcole());
        nouvelle.setClasse(nouvelleClasse);
        nouvelle.setAnneeScolaire(active);
        nouvelle.setCreatedAt(LocalDateTime.now());
        nouvelle.setStatut(StatutInscription.PREINSCRIT);

        // La décision du conseil est conservée dans le
        // résultat annuel. Ne pas déduire la décision
        // de la seule différence entre les classes.

        Inscription sauvegardee =
                inscriptionRepository.save(nouvelle);

        ligneFraisService.genererLignesFrais(sauvegardee);

        return sauvegardee;
    }




    private boolean estCyclePrimaire(Cycle cycle) {

        if (cycle == null || cycle.getNom() == null) {
            return false;
        }

        String nom = java.text.Normalizer
                .normalize(
                        cycle.getNom().trim(),
                        java.text.Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .toUpperCase(java.util.Locale.ROOT)
                .replaceAll("[_\\s-]+", " ")
                .trim();

        return nom.equals("PRIMAIRE")
                || nom.equals("PREMIER CYCLE");
    }

    private void verifierClassePrimaire(Classe classe) {

        if (classe == null
                || classe.getNiveau() == null
                || !estCyclePrimaire(
                classe.getNiveau().getCycle()
        )) {

            throw new IllegalArgumentException(
                    "Cette classe n'appartient pas au cycle primaire."
            );
        }
    }

}
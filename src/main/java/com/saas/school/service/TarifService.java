package com.saas.school.service;

import com.saas.school.dto.TarifResponseDTO;
import com.saas.school.entity.*;
import com.saas.school.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TarifService {

    private final TarifRepository tarifRepository;
    private final TypeFraisRepository typeFraisRepository;
    private final NiveauRepository niveauRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;
    private final EcoleRepository ecoleRepository;
    private final InscriptionRepository inscriptionRepository;
    private final LigneFraisService ligneFraisService;

    public Tarif creerOuModifierTarif(
            Long ecoleId,
            Long niveauId,
            Long anneeId,
            String codeTypeFrais,
            Double montant
    ) {

        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() -> new RuntimeException("Ecole introuvable"));

        Niveau niveau = niveauRepository.findById(niveauId)
                .orElseThrow(() -> new RuntimeException("Niveau introuvable"));

        AnneeScolaire annee = anneeScolaireRepository.findById(anneeId)
                .orElseThrow(() -> new RuntimeException("Année introuvable"));

        TypeFrais typeFrais = typeFraisRepository
                .findByEcoleIdAndCode(ecoleId, codeTypeFrais)
                .orElseThrow(() -> new RuntimeException("Type frais introuvable"));

        Tarif tarif = tarifRepository
                .findByNiveauIdAndAnneeScolaireIdAndTypeFrais_Code(
                        niveauId,
                        anneeId,
                        codeTypeFrais
                )
                .orElse(new Tarif());

        tarif.setEcole(ecole);
        tarif.setNiveau(niveau);
        tarif.setAnneeScolaire(annee);
        tarif.setTypeFrais(typeFrais);
        tarif.setMontant(montant);

        Tarif savedTarif = tarifRepository.save(tarif);

        // 🔥 Recalcule automatiquement les lignes de frais estimatives concernées
        int nbLignesMaj = ligneFraisService.recalculerLignesEstimatives(
                niveauId, anneeId, codeTypeFrais, montant
        );

        System.out.println(nbLignesMaj + " ligne(s) de frais recalculée(s) suite à la mise à jour du tarif "
                + codeTypeFrais + " pour le niveau " + niveau.getNom());

        return savedTarif;
    }

    public List<TarifResponseDTO> getByEcole(Long ecoleId) {
        return tarifRepository.findByEcoleId(ecoleId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<TarifResponseDTO> getByEcoleAndAnnee(Long ecoleId, Long anneeId) {
        return tarifRepository.findByEcoleIdAndAnneeScolaireId(ecoleId, anneeId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public Tarif getById(Long id) {
        return tarifRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarif introuvable"));
    }

    public void supprimer(Long id) {
        tarifRepository.deleteById(id);
    }

    private TarifResponseDTO mapToDto(Tarif t) {

        TarifResponseDTO dto = new TarifResponseDTO();
        dto.setId(t.getId());
        dto.setMontant(t.getMontant());
        dto.setEcoleId(t.getEcole().getId());

        if (t.getNiveau() != null) {
            dto.setNiveauId(t.getNiveau().getId());
            dto.setNiveauNom(t.getNiveau().getNom());
        }

        if (t.getAnneeScolaire() != null) {
            dto.setAnneeScolaireId(t.getAnneeScolaire().getId());
            dto.setAnneeNom(t.getAnneeScolaire().getNom());
        }

        if (t.getTypeFrais() != null) {
            dto.setTypeFraisId(t.getTypeFrais().getId());
            dto.setTypeFraisCode(t.getTypeFrais().getCode());
            dto.setTypeFraisLibelle(t.getTypeFrais().getLibelle());
        }

        return dto;
    }
}
package com.saas.school.service;


import com.saas.school.entity.AnneeScolaire;
import com.saas.school.entity.Ecole;
import com.saas.school.entity.Niveau;
import com.saas.school.entity.TypeFrais;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EcoleService {

    private final EcoleRepository ecoleRepository;
    private final NiveauRepository niveauRepository;
    private final TypeFraisRepository typeFraisRepository;
    private final TarifRepository tarifRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;

    public Ecole creerEcole(Ecole ecole) {
        ecole.setCreatedAt(LocalDateTime.now());
        ecole.setActive(true);

        return ecoleRepository.save(ecole);
    }

    public Ecole getById(Long id) {
        return ecoleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ecole introuvable"));
    }
    // 🔴 activer / désactiver école
    public Ecole toggleActive(Long id) {
        Ecole ecole = ecoleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        ecole.setActive(!ecole.isActive());
        return ecoleRepository.save(ecole);
    }
    // 📋 liste écoles
    public List<Ecole> getAllEcoles() {
        return ecoleRepository.findAll();
    }

    public boolean tousLesTarifsSontConfigures(Long ecoleId) {

        List<Niveau> niveaux = niveauRepository.findByEcoleId(ecoleId);
        List<TypeFrais> typesFrais = typeFraisRepository.findByEcoleId(ecoleId);

        if (niveaux.isEmpty()) {
            return true; // pas encore de niveau créé → rien à signaler pour l'instant
        }

        AnneeScolaire anneeActive = anneeScolaireRepository
                .findByEcoleIdAndActiveTrue(ecoleId)
                .orElse(null);

        if (anneeActive == null) return false;

        for (Niveau niveau : niveaux) {
            for (TypeFrais type : typesFrais) {
                boolean existe = tarifRepository
                        .findByNiveauIdAndAnneeScolaireIdAndTypeFrais_Code(
                                niveau.getId(), anneeActive.getId(), type.getCode()
                        )
                        .isPresent();

                if (!existe) return false;
            }
        }

        return true;
    }

    public Ecole modifierEcole(
            Long id,
            String nom,
            String codeEcole,
            String adresse,
            String ville,
            String pays,
            String telephone,
            String email,
            MultipartFile logo
    ) {

        Ecole ecole = ecoleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        // ==========================================
        // INFORMATIONS GÉNÉRALES
        // ==========================================

        if (nom != null) {
            ecole.setNom(nom);
        }

        if (codeEcole != null) {
            ecole.setCodeEcole(codeEcole);
        }

        if (adresse != null) {
            ecole.setAdresse(adresse);
        }

        if (ville != null) {
            ecole.setVille(ville);
        }

        if (pays != null) {
            ecole.setPays(pays);
        }

        if (telephone != null) {
            ecole.setTelephone(telephone);
        }

        if (email != null) {
            ecole.setEmail(email);
        }

        // ==========================================
        // LOGO
        // ==========================================

        if (logo != null && !logo.isEmpty()) {

            try {

                Path dossier = Paths.get("uploads/ecoles");

                if (!Files.exists(dossier)) {
                    Files.createDirectories(dossier);
                }

                String originalName = logo.getOriginalFilename();

                String extension = "";

                if (originalName != null && originalName.contains(".")) {
                    extension = originalName.substring(
                            originalName.lastIndexOf(".")
                    ).toLowerCase();
                }

                // Nom unique du fichier
                String nomFichier =
                        "logo-" + id + "-" + UUID.randomUUID() + extension;

                Path chemin = dossier.resolve(nomFichier);

                // Enregistrement du fichier
                Files.copy(
                        logo.getInputStream(),
                        chemin
                );

                // On stocke uniquement le chemin en DB
                ecole.setLogo(
                        "/uploads/ecoles/" + nomFichier
                );

            } catch (IOException e) {

                throw new RuntimeException(
                        "Erreur lors de l'enregistrement du logo",
                        e
                );
            }
        }

        return ecoleRepository.save(ecole);
    }
}

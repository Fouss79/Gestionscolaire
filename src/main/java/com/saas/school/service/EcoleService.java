package com.saas.school.service;

import com.saas.school.entity.AnneeScolaire;
import com.saas.school.entity.Ecole;
import com.saas.school.entity.Niveau;
import com.saas.school.entity.TypeFrais;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EcoleService {

    private final EcoleRepository ecoleRepository;
    private final NiveauRepository niveauRepository;
    private final TypeFraisRepository typeFraisRepository;
    private final TarifRepository tarifRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;

    /*
     * Service Supabase utilisé pour stocker
     * les logos et les images.
     */
    private final SupabaseStorageService supabaseStorageService;


    // ============================================================
    // CRÉATION ÉCOLE
    // ============================================================

    public Ecole creerEcole(Ecole ecole) {

        ecole.setCreatedAt(LocalDateTime.now());
        ecole.setActive(true);

        return ecoleRepository.save(ecole);
    }


    // ============================================================
    // RÉCUPÉRER UNE ÉCOLE
    // ============================================================

    public Ecole getById(Long id) {

        return ecoleRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Ecole introuvable"
                        )
                );
    }


    // ============================================================
    // ACTIVER / DÉSACTIVER
    // ============================================================

    public Ecole toggleActive(Long id) {

        Ecole ecole =
                ecoleRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "École introuvable"
                                )
                        );

        ecole.setActive(!ecole.isActive());

        return ecoleRepository.save(ecole);
    }


    // ============================================================
    // LISTE DES ÉCOLES
    // ============================================================

    public List<Ecole> getAllEcoles() {

        return ecoleRepository.findAll();
    }


    // ============================================================
    // VÉRIFICATION DES TARIFS
    // ============================================================

    public boolean tousLesTarifsSontConfigures(Long ecoleId) {

        List<Niveau> niveaux =
                niveauRepository.findByEcoleId(ecoleId);

        List<TypeFrais> typesFrais =
                typeFraisRepository.findByEcoleId(ecoleId);

        if (niveaux.isEmpty()) {

            return true;
        }

        AnneeScolaire anneeActive =
                anneeScolaireRepository
                        .findByEcoleIdAndActiveTrue(ecoleId)
                        .orElse(null);

        if (anneeActive == null) {

            return false;
        }

        for (Niveau niveau : niveaux) {

            for (TypeFrais type : typesFrais) {

                boolean existe =
                        tarifRepository
                                .findByNiveauIdAndAnneeScolaireIdAndTypeFrais_Code(
                                        niveau.getId(),
                                        anneeActive.getId(),
                                        type.getCode()
                                )
                                .isPresent();

                if (!existe) {

                    return false;
                }
            }
        }

        return true;
    }


    // ============================================================
    // MODIFICATION ÉCOLE
    // ============================================================

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

        Ecole ecole =
                ecoleRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "École introuvable"
                                )
                        );


        // ========================================================
        // INFORMATIONS GÉNÉRALES
        // ========================================================

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


        // ========================================================
        // LOGO → SUPABASE STORAGE
        // ========================================================

        if (logo != null && !logo.isEmpty()) {

            try {

                /*
                 * Le logo sera organisé comme ceci :
                 *
                 * dani-images/
                 * └── ecoles/
                 *     └── 52/
                 *         └── uuid.png
                 */

                String dossier =
                        "ecoles/" + id;

                String logoUrl =
                        supabaseStorageService.uploadImage(
                                logo,
                                dossier
                        );

                /*
                 * On stocke l'URL publique Supabase
                 * dans la colonne logo.
                 */
                ecole.setLogo(logoUrl);

            } catch (Exception e) {

                throw new RuntimeException(
                        "Impossible d'enregistrer le logo : "
                                + e.getMessage(),
                        e
                );
            }
        }


        // ========================================================
        // SAUVEGARDE
        // ========================================================

        return ecoleRepository.save(ecole);
    }
}
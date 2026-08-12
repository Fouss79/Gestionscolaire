package com.saas.school.service;

import com.saas.school.dto.EleveRequest;
import com.saas.school.dto.EleveResponseDTO;
import com.saas.school.entity.Classe;
import com.saas.school.entity.Ecole;
import com.saas.school.entity.Eleve;
import com.saas.school.entity.Utilisateur;
import com.saas.school.repository.ClasseRepository;
import com.saas.school.repository.EcoleRepository;
import com.saas.school.repository.EleveRepository;
import com.saas.school.repository.InscriptionRepository;
import com.saas.school.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.saas.school.entity.SousGroupe;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EleveService {

    private final EleveRepository eleveRepository;
    private final ClasseRepository classeRepository;
    private final EcoleRepository ecoleRepository;
    private final InscriptionRepository inscriptionRepository;
    private final UtilisateurRepository utilisateurRepository;

    // 🔥 Créer élève
    public Eleve creerEleve(EleveRequest request) {

        Ecole ecole = ecoleRepository.findById(request.getEcoleId())
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        Classe classe = classeRepository.findById(request.getClasseId())
                .orElseThrow(() -> new RuntimeException("Classe introuvable"));

        // génération matricule
        long count = eleveRepository.count() + 1;
        String matricule = "ELV" + String.format("%04d", count);

        Eleve eleve = new Eleve();

        // --- Identité ---
        eleve.setNom(request.getNom());
        eleve.setPrenom(request.getPrenom());
        eleve.setDateNaissance(request.getDateNaissance());
        eleve.setSexe(request.getSexe());
        eleve.setLieuNaissance(request.getLieuNaissance());
        eleve.setNationalite(request.getNationalite());
        eleve.setNumeroExtraitNaissance(request.getNumeroExtraitNaissance());
        eleve.setGroupeSanguin(request.getGroupeSanguin());
        eleve.setAllergiesMaladies(request.getAllergiesMaladies());

        // --- Coordonnées élève ---
        eleve.setAdresse(request.getAdresse());
        eleve.setTelephone(request.getTelephone());
        eleve.setEmail(request.getEmail());

        // --- Tuteur / Parent ---
        eleve.setNomTuteur(request.getNomTuteur());
        eleve.setPrenomTuteur(request.getPrenomTuteur());
        eleve.setLienParente(request.getLienParente());
        eleve.setTelephoneTuteur(request.getTelephoneTuteur());
        eleve.setEmailTuteur(request.getEmailTuteur());
        eleve.setProfessionTuteur(request.getProfessionTuteur());
        eleve.setAdresseTuteur(request.getAdresseTuteur());

        // --- Administratif ---
        eleve.setMatricule(matricule);
        eleve.setCreatedAt(LocalDateTime.now());
        eleve.setActive(true);
        eleve.setEcole(ecole);
        eleve.setClasse(classe);

        // --- Compte utilisateur lié (corrige un bug : jamais assigné auparavant) ---
        if (request.getUtilisateurId() != null) {
            Utilisateur utilisateur = utilisateurRepository.findById(request.getUtilisateurId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
            eleve.setUtilisateur(utilisateur);
        }

        return eleveRepository.save(eleve);
    }



    // 📥 élèves par école
    public List<Eleve> getByEcole(Long ecoleId) {
        return eleveRepository.findByEcoleId(ecoleId);
    }

    // 📥 par id
    public Eleve getById(Long id) {
        return eleveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Élève introuvable"));
    }
    public List<EleveResponseDTO> getByClasse(Long classeId) {
        return eleveRepository.findByClasseId(classeId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private EleveResponseDTO mapToDto(Eleve e) {

        EleveResponseDTO dto = new EleveResponseDTO();
        dto.setId(e.getId());
        dto.setNom(e.getNom());
        dto.setPrenom(e.getPrenom());
        dto.setSexe(e.getSexe());
        dto.setNumeroMatricule(e.getMatricule());
        dto.setDateNaissance(e.getDateNaissance());
        dto.setPhotoUrl(e.getPhotoUrl());
        dto.setStatut(e.getStatut() != null ? e.getStatut().name() : null);

        if (e.getClasse() != null) {
            dto.setClasseId(e.getClasse().getId());
            dto.setClasseNom(e.getClasse().getNomComplet());
        }

        dto.setSousGroupeIds(
                e.getSousGroupes().stream()
                        .map(SousGroupe::getId)
                        .toList()
        );

        return dto;
    }
}
package com.saas.school.service;


import com.saas.school.dto.InscriptionDTO;
import com.saas.school.dto.InscriptionResponseDTO;
import com.saas.school.dto.EleveResponseDTO;
import com.saas.school.dto.EleveRequest;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InscriptionService {

    private final EcoleRepository ecoleRepository;
    private final EleveRepository eleveRepository;
    private final EleveService eleveService;
    private final ClasseRepository classeRepository;
    private final InscriptionRepository inscriptionRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;
    private final AbonnementService abonnementService;


    public Inscription inscrireUnEleve(InscriptionDTO request) {

        // 🔥 0. vérifier abonnement AVANT tout
        Ecole ecole = ecoleRepository.findById(request.getEcoleId())
                .orElseThrow(() -> new RuntimeException("École introuvable"));





        // 🔥 1. créer élève
        EleveRequest eleveReq = new EleveRequest();
        eleveReq.setNom(request.getNom());
        eleveReq.setPrenom(request.getPrenom());
        eleveReq.setDateNaissance(request.getDateNaissance());
        eleveReq.setSexe(request.getSexe());
        eleveReq.setEcoleId(request.getEcoleId());
        eleveReq.setClasseId(request.getClasseId());

        Eleve eleve = eleveService.creerEleve(eleveReq);

        // 🔥 2. classe
        Classe classe = classeRepository.findById(request.getClasseId())
                .orElseThrow(() -> new RuntimeException("Classe introuvable"));

        // 🔥 3. année active
        AnneeScolaire anneeActive = anneeScolaireRepository
                .findByEcoleIdAndActiveTrue(request.getEcoleId())
                .orElseThrow(() -> new RuntimeException("Aucune année active"));

        // 🔥 4. inscription
        Inscription inscription = new Inscription();
        inscription.setEleve(eleve);
        inscription.setClasse(classe);
        inscription.setAnneeScolaire(anneeActive);
        inscription.setEcole(ecole);
        inscription.setCreatedAt(LocalDateTime.now());
        inscription.setActive(true);
        // 🔥 AVANT toute action


        if (!abonnementService.isActif(ecole)) {
            throw new RuntimeException("Abonnement expiré");
        }

        return inscriptionRepository.save(inscription);
    }

    public List<InscriptionResponseDTO> getInscriptionsByEcoleAndAnneeActive(Long ecoleId) {

        // 🔥 1. récupérer année active
        AnneeScolaire anneeActive = anneeScolaireRepository
                .findByEcoleIdAndActiveTrue(ecoleId)
                .orElseThrow(() -> new RuntimeException("Aucune année active"));

        // 🔥 2. récupérer inscriptions
        return inscriptionRepository
                .findByEcoleIdAndAnneeScolaire_Id(ecoleId, anneeActive.getId())
                .stream()
                .map(i -> {
                    InscriptionResponseDTO dto = new InscriptionResponseDTO();

                    dto.setId(i.getId());
                    dto.setNom(i.getEleve().getNom());
                    dto.setPrenom(i.getEleve().getPrenom());
                    dto.setMatricule(i.getEleve().getMatricule());

                    dto.setClasseNom(i.getClasse().getNomComplet());
                    dto.setAnnee(i.getAnneeScolaire().getNom());

                    dto.setDateInscription(i.getCreatedAt());
                    dto.setStatut(i.isActive() ? "INSCRIT" : "PREINSCRIT");

                    return dto;
                })
                .toList();
    }
    public List<EleveResponseDTO> getElevesByEcoleAndAnneeActive(Long ecoleId) {

        AnneeScolaire anneeActive = anneeScolaireRepository
                .findByEcoleIdAndActiveTrue(ecoleId)
                .orElseThrow(() -> new RuntimeException("Aucune année active"));

        return inscriptionRepository
                .findByEcoleIdAndAnneeScolaire_Id(ecoleId, anneeActive.getId())
                .stream()
                .map(i -> {

                    Eleve e = i.getEleve();

                    EleveResponseDTO dto = new EleveResponseDTO();

                    dto.setId(e.getId());
                    dto.setNom(e.getNom());
                    dto.setPrenom(e.getPrenom());
                    dto.setSexe(e.getSexe());

                    dto.setNumeroMatricule(e.getMatricule());

                    dto.setClasseNom(i.getClasse().getNomComplet());
                    dto.setClasseId(i.getClasse().getId());

                    dto.setAnneeScolaire(i.getAnneeScolaire().getNom());

                    dto.setStatut(i.isActive() ? "INSCRIT" : "PREINSCRIT");

                    dto.setDateInscription(i.getCreatedAt().toString());

                    return dto;

                })
                .toList();
    }

    public List<InscriptionResponseDTO> getInscriptions() {

        return inscriptionRepository.findAll().stream().map(i -> {
            InscriptionResponseDTO dto = new InscriptionResponseDTO();

            dto.setId(i.getId());
            dto.setNom(i.getEleve().getNom());
            dto.setPrenom(i.getEleve().getPrenom());
            dto.setMatricule(i.getEleve().getMatricule());

            dto.setClasseNom(i.getClasse().getNomComplet());
            dto.setAnnee(i.getAnneeScolaire().getNom());

            dto.setDateInscription(i.getCreatedAt());
            dto.setStatut(i.isActive() ? "INSCRIT" : "PREINSCRIT");

            return dto;
        }).toList();
    }

    public List<EleveResponseDTO> getAllEleves() {

        List<Inscription> inscriptions = inscriptionRepository.findAll();

        return inscriptions.stream().map(i -> {

            Eleve e = i.getEleve();

            EleveResponseDTO dto = new EleveResponseDTO();

            dto.setId(e.getId());
            dto.setNom(e.getNom());
            dto.setPrenom(e.getPrenom());
            dto.setSexe(e.getSexe());

            dto.setNumeroMatricule(e.getMatricule());

            dto.setClasseNom(i.getClasse().getNomComplet());
            dto.setClasseId(i.getClasse().getId());

            dto.setAnneeScolaire(i.getAnneeScolaire().getNom());

            dto.setStatut(i.isActive() ? "INSCRIT" : "PREINSCRIT");

            dto.setDateInscription(i.getCreatedAt().toString());

            return dto;

        }).toList();
    }


}
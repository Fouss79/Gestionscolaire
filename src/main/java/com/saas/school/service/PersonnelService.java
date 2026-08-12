package com.saas.school.service;


import com.saas.school.dto.PersonnelRequest;
import com.saas.school.dto.PersonnelResponseDTO;
import com.saas.school.entity.*;
import com.saas.school.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class PersonnelService {


    private final PersonnelRepository personnelRepository;
    private final EcoleRepository ecoleRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;



    public Personnel creerPersonnel(PersonnelRequest request){


        Ecole ecole = ecoleRepository.findById(request.getEcoleId())
                .orElseThrow(
                        () -> new RuntimeException("Ecole introuvable")
                );

        System.out.println("ROLE RECU = " + request.getRole());
        System.out.println("ECOLE ID = " + ecole.getId());

        Role role = roleRepository
                .findByNomAndEcoleId(
                        request.getRole(),
                        ecole.getId()
                )
                .orElseThrow(
                        () -> new RuntimeException("Role introuvable")
                );


        // Création compte utilisateur

        Utilisateur utilisateur =creerCompteUtilisateurPersonnel(request.getNom(),request.getPrenom(),ecole,role);


        // Création personnel


        Personnel personnel = new Personnel();


        personnel.setNom(request.getNom());
        personnel.setPrenom(request.getPrenom());

        personnel.setDateNaissance(
                request.getDateNaissance()
        );

        personnel.setLieuNaissance(
                request.getLieuNaissance()
        );

        personnel.setSexe(
                request.getSexe()
        );

        personnel.setNationalite(
                request.getNationalite()
        );


        personnel.setTelephone(
                request.getTelephone()
        );

        personnel.setTelephoneSecondaire(
                request.getTelephoneSecondaire()
        );


        personnel.setEmail(
                request.getEmail()
        );


        personnel.setAdresse(
                request.getAdresse()
        );


        personnel.setContactUrgenceNom(
                request.getContactUrgenceNom()
        );


        personnel.setContactUrgenceTelephone(
                request.getContactUrgenceTelephone()
        );


        personnel.setDateEmbauche(
                request.getDateEmbauche()
        );


        personnel.setDateFinContrat(
                request.getDateFinContrat()
        );


        personnel.setMatricule(
                genererMatricule()
        );


        personnel.setActif(true);

        personnel.setEcole(ecole);

        personnel.setUtilisateur(utilisateur);



        return personnelRepository.save(personnel);

    }




    public List<PersonnelResponseDTO> getByEcole(Long ecoleId){

        return personnelRepository.findByEcoleId(ecoleId)
                .stream()
                .map(this::mapDto)
                .toList();

    }





    public Personnel getById(Long id){

        return personnelRepository.findById(id)
                .orElseThrow(
                        () -> new RuntimeException("Personnel introuvable")
                );
    }





    public Personnel modifier(Long id, PersonnelRequest request){


        Personnel p = getById(id);


        p.setNom(request.getNom());
        p.setPrenom(request.getPrenom());

        p.setTelephone(request.getTelephone());

        p.setEmail(request.getEmail());

        p.setAdresse(request.getAdresse());


        return personnelRepository.save(p);

    }




    private PersonnelResponseDTO mapDto(Personnel p){

        PersonnelResponseDTO dto =
                new PersonnelResponseDTO();


        dto.setId(p.getId());

        dto.setNom(p.getNom());

        dto.setPrenom(p.getPrenom());

        dto.setMatricule(
                p.getMatricule()
        );

        dto.setTelephone(
                p.getTelephone()
        );

        dto.setEmail(
                p.getEmail()
        );


        dto.setActif(
                p.getActif()
        );


        dto.setRole(
                p.getUtilisateur()
                        .getRole()
                        .getNom()
        );


        return dto;

    }




    private String genererMatricule(){

        return "PER"
                +
                (personnelRepository.count()+1);

    }



    private String genererEmail(String nom,String prenom){

        return prenom.toLowerCase()
                +"."+nom.toLowerCase()
                +"@ecole.local";

    }


    private Utilisateur creerCompteUtilisateurPersonnel(
            String nom,
            String prenom,
            Ecole ecole,
            Role role

    ){

        Utilisateur utilisateur = new Utilisateur();

        utilisateur.setNom(nom);

        utilisateur.setEmail(
                genererEmailUnique(nom, prenom)
        );

        String motDePasse = genererMotDePasseTemporaire();

        utilisateur.setPassword(
                passwordEncoder.encode(motDePasse)
        );

        utilisateur.setMotDePasseTemporaire(motDePasse);
        utilisateur.setRole(role);
        utilisateur.setEcole(ecole);


        return utilisateurRepository.save(utilisateur);
    }


    private String genererEmailUnique(
            String nom,
            String prenom
    ){

        String base =
                prenom.toLowerCase()
                        +"."+nom.toLowerCase();

        return base
                + UUID.randomUUID()
                .toString()
                .substring(0,5)
                +"@ecole.local";
    }

    private String genererMotDePasseTemporaire() {
        return UUID.randomUUID().toString().substring(0, 10);
    }
}
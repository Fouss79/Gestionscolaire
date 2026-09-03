package com.saas.school.service;

import com.saas.school.dto.EnseignantRequest;
import com.saas.school.dto.EnseignantResponseDTO;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnseignantService {

    private static final String ROLE_ENSEIGNANT = "ENSEIGNANT";
    private final EnseignantRepository enseignantRepository;
    private final MatiereRepository matiereRepository;
    private final EcoleRepository ecoleRepository;
    private final RoleRepository roleRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public Enseignant creerEnseignant(EnseignantRequest request) {

        Ecole ecole = ecoleRepository.findById(request.getEcoleId())
                .orElseThrow(() -> new RuntimeException("École introuvable"));
        Utilisateur utilisateur =
                creerCompteUtilisateurEnseignant(
                        request.getNom(),
                        request.getPrenom(),
                        ecole
                );

        Set<Matiere> matieres = new HashSet<>();
        if (request.getMatiereIds() != null && !request.getMatiereIds().isEmpty()) {
            matieres.addAll(matiereRepository.findAllById(request.getMatiereIds()));
        }

        Enseignant enseignant = new Enseignant();

        enseignant.setNom(request.getNom());
        enseignant.setPrenom(request.getPrenom());
        enseignant.setDateNaissance(request.getDateNaissance());
        enseignant.setLieuNaissance(request.getLieuNaissance());
        enseignant.setSexe(request.getSexe());
        enseignant.setNationalite(request.getNationalite());
        enseignant.setUtilisateur(utilisateur);
        enseignant.setTelephone(request.getTelephone());
        enseignant.setTelephoneSecondaire(request.getTelephoneSecondaire());
        enseignant.setEmail(request.getEmail());
        enseignant.setAdresse(request.getAdresse());

        enseignant.setContactUrgenceNom(request.getContactUrgenceNom());
        enseignant.setContactUrgenceTelephone(request.getContactUrgenceTelephone());

        enseignant.setSpecialite(request.getSpecialite());

        if (request.getNiveauDiplome() != null) {
            enseignant.setNiveauDiplome(Enseignant.NiveauDiplome.valueOf(request.getNiveauDiplome()));
        }
        enseignant.setDiplomeObtenu(request.getDiplomeObtenu());

        if (request.getTypeContrat() != null) {
            enseignant.setTypeContrat(Enseignant.TypeContrat.valueOf(request.getTypeContrat()));
        }
        enseignant.setDateEmbauche(request.getDateEmbauche());
        enseignant.setDateFinContrat(request.getDateFinContrat());
        enseignant.setSalaireBase(request.getSalaireBase());
        enseignant.setTauxHoraire(request.getTauxHoraire());
        enseignant.setNombreHeuresParSemaine(request.getNombreHeuresParSemaine());

        enseignant.setMatieresEnseignees(matieres);
        enseignant.setEcole(ecole);
        enseignant.setMatricule(genererMatricule());
        enseignant.setActif(true);

        return enseignantRepository.save(enseignant);
    }

    public List<EnseignantResponseDTO> getByEcole(Long ecoleId) {
        return enseignantRepository.findByEcoleId(ecoleId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<EnseignantResponseDTO> getActifsByEcole(Long ecoleId) {
        return enseignantRepository.findByEcoleIdAndActifTrue(ecoleId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public Enseignant getById(Long id) {
        return enseignantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enseignant introuvable"));
    }

    public Enseignant toggleActif(Long id) {
        Enseignant enseignant = getById(id);
        enseignant.setActif(!enseignant.getActif());
        return enseignantRepository.save(enseignant);
    }

    private String genererMatricule() {
        long count = enseignantRepository.count() + 1;
        return "ENS" + String.format("%04d", count);
    }

    private EnseignantResponseDTO mapToDto(Enseignant e) {

        EnseignantResponseDTO dto = new EnseignantResponseDTO();
        dto.setId(e.getId());
        dto.setNom(e.getNom());
        dto.setPrenom(e.getPrenom());
        dto.setMatricule(e.getMatricule());
        dto.setDateNaissance(e.getDateNaissance());
        dto.setSexe(e.getSexe());
        dto.setTelephone(e.getTelephone());
        dto.setEmail(e.getEmail());
        dto.setSpecialite(e.getSpecialite());
        dto.setNiveauDiplome(e.getNiveauDiplome() != null ? e.getNiveauDiplome().name() : null);
        dto.setTypeContrat(e.getTypeContrat() != null ? e.getTypeContrat().name() : null);
        dto.setSalaireBase(e.getSalaireBase());
        dto.setTauxHoraire(e.getTauxHoraire());
        dto.setActif(e.getActif());

        dto.setMatiereIds(e.getMatieresEnseignees().stream().map(Matiere::getId).toList());
        dto.setMatiereNoms(e.getMatieresEnseignees().stream().map(Matiere::getNom).toList());

        return dto;
    }

    public Enseignant modifierEnseignant(Long id, EnseignantRequest request) {

        Enseignant enseignant = getById(id);

        enseignant.setNom(request.getNom());
        enseignant.setPrenom(request.getPrenom());
        enseignant.setDateNaissance(request.getDateNaissance());
        enseignant.setLieuNaissance(request.getLieuNaissance());
        enseignant.setSexe(request.getSexe());
        enseignant.setNationalite(request.getNationalite());

        enseignant.setTelephone(request.getTelephone());
        enseignant.setTelephoneSecondaire(request.getTelephoneSecondaire());
        enseignant.setEmail(request.getEmail());
        enseignant.setAdresse(request.getAdresse());

        enseignant.setContactUrgenceNom(request.getContactUrgenceNom());
        enseignant.setContactUrgenceTelephone(request.getContactUrgenceTelephone());

        enseignant.setSpecialite(request.getSpecialite());

        if (request.getNiveauDiplome() != null) {
            enseignant.setNiveauDiplome(Enseignant.NiveauDiplome.valueOf(request.getNiveauDiplome()));
        }
        enseignant.setDiplomeObtenu(request.getDiplomeObtenu());

        if (request.getTypeContrat() != null) {
            enseignant.setTypeContrat(Enseignant.TypeContrat.valueOf(request.getTypeContrat()));
        }
        enseignant.setDateEmbauche(request.getDateEmbauche());
        enseignant.setDateFinContrat(request.getDateFinContrat());
        enseignant.setSalaireBase(request.getSalaireBase());
        enseignant.setTauxHoraire(request.getTauxHoraire());
        enseignant.setNombreHeuresParSemaine(request.getNombreHeuresParSemaine());

        if (request.getMatiereIds() != null) {
            Set<Matiere> matieres = new HashSet<>(matiereRepository.findAllById(request.getMatiereIds()));
            enseignant.setMatieresEnseignees(matieres);
        }

        return enseignantRepository.save(enseignant);
    }

    public EnseignantResponseDTO getByIdDto(Long id) {
        return mapToDto(getById(id));
    }

    private Role obtenirOuCreerRoleEnseignant(Ecole ecole){

        return roleRepository
                .findByNomAndEcoleId("ENSEIGNANT", ecole.getId())
                .orElseGet(() -> {

                    Role role = new Role();

                    role.setNom("ENSEIGNANT");
                    role.setEcole(ecole);

                    return roleRepository.save(role);
                });
    }
    private Utilisateur creerCompteUtilisateurEnseignant(
            String nom,
            String prenom,
            Ecole ecole
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
        utilisateur.setRole(obtenirOuCreerRoleEnseignant(ecole));
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
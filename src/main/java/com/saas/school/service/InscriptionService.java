package com.saas.school.service;

import com.saas.school.dto.*;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InscriptionService {

    private static final String ROLE_ELEVE = "ELEVE";
    private static final String DOMAINE_COMPTE_ELEVE = "ecole.local";

    private final EcoleRepository ecoleRepository;
    private final EleveService eleveService;
    private final ClasseRepository classeRepository;
    private final InscriptionRepository inscriptionRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;
    private final AbonnementService abonnementService;
    private final EleveRepository eleveRepository;
    private final NoteService noteService;
    private final PasswordEncoder passwordEncoder;
    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final LigneFraisService ligneFraisService;
    private final LigneFraisRepository ligneFraisRepository;

    // =========================
    // 🟡 PREINSCRIPTION
    // =========================

    @Transactional
    public Inscription inscrireUnEleve(InscriptionDTO request) {

        Ecole ecole = ecoleRepository.findById(request.getEcoleId())
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        if (!abonnementService.isActif(ecole)) {
            throw new RuntimeException("Abonnement expiré");
        }

        AnneeScolaire anneeActive = anneeScolaireRepository
                .findByEcoleIdAndActiveTrue(ecole.getId())
                .orElseThrow(() -> new RuntimeException("Aucune année active"));

        Classe classe = classeRepository.findById(request.getClasseId())
                .orElseThrow(() -> new RuntimeException("Classe introuvable"));

        // 🔥 USER
        Utilisateur utilisateur = creerCompteUtilisateurEleve(request.getNom(), request.getPrenom(), ecole);

        // 🔥 ELEVE
        EleveRequest eleveReq = new EleveRequest();

        eleveReq.setNom(request.getNom());
        eleveReq.setPrenom(request.getPrenom());
        eleveReq.setDateNaissance(request.getDateNaissance());
        eleveReq.setSexe(request.getSexe());
        eleveReq.setLieuNaissance(request.getLieuNaissance());
        eleveReq.setNationalite(request.getNationalite());
        eleveReq.setNumeroExtraitNaissance(request.getNumeroExtraitNaissance());
        eleveReq.setGroupeSanguin(request.getGroupeSanguin());
        eleveReq.setAllergiesMaladies(request.getAllergiesMaladies());

        eleveReq.setAdresse(request.getAdresse());
        eleveReq.setTelephone(request.getTelephone());
        eleveReq.setEmail(request.getEmail());

        eleveReq.setNomTuteur(request.getNomTuteur());
        eleveReq.setPrenomTuteur(request.getPrenomTuteur());
        eleveReq.setLienParente(request.getLienParente());
        eleveReq.setTelephoneTuteur(request.getTelephoneTuteur());
        eleveReq.setEmailTuteur(request.getEmailTuteur());
        eleveReq.setProfessionTuteur(request.getProfessionTuteur());
        eleveReq.setAdresseTuteur(request.getAdresseTuteur());

        eleveReq.setEcoleId(request.getEcoleId());
        eleveReq.setClasseId(request.getClasseId());
        eleveReq.setUtilisateurId(utilisateur.getId());

        Eleve eleve = eleveService.creerEleve(eleveReq);

        // 🔥 INSCRIPTION
        Inscription inscription = new Inscription();
        inscription.setEleve(eleve);
        inscription.setClasse(classe);
        inscription.setAnneeScolaire(anneeActive);
        inscription.setEcole(ecole);
        inscription.setCreatedAt(LocalDateTime.now());
        inscription.setStatut(StatutInscription.PREINSCRIT);

        Inscription savedInscription = inscriptionRepository.save(inscription);

        // 🔥 Génère une ligne de frais par type (INSCRIPTION, SCOLARITE, EXAMEN, UNIFORME...)
        ligneFraisService.genererLignesFrais(savedInscription);

        return savedInscription;
    }

    public List<EleveResponseDTO> getElevesReinscription(Long ecoleId) {

        return inscriptionRepository.findByEcoleId(ecoleId)
                .stream()
                .map(Inscription::getEleve)
                .distinct()
                .map(eleve -> {
                    EleveResponseDTO dto = new EleveResponseDTO();
                    dto.setId(eleve.getId());
                    dto.setNom(eleve.getNom());
                    dto.setPrenom(eleve.getPrenom());
                    dto.setNumeroMatricule(eleve.getMatricule());
                    return dto;
                })
                .toList();
    }

    // =========================
    // 📋 LISTE ECOLE + ANNEE ACTIVE
    // =========================
    public List<InscriptionResponseDTO> getInscriptionsByEcoleAndAnneeActive(Long ecoleId) {

        AnneeScolaire anneeActive = anneeScolaireRepository
                .findByEcoleIdAndActiveTrue(ecoleId)
                .orElseThrow(() -> new RuntimeException("Aucune année active"));

        return inscriptionRepository
                .findByEcoleIdAndAnneeScolaire_Id(ecoleId, anneeActive.getId())
                .stream()
                .map(this::mapInscription)
                .toList();
    }

    public List<InscriptionResponseDTO> getByClasseAndAnnee(Long classeId, Long anneeId) {

        return inscriptionRepository
                .findByClasseIdAndAnneeScolaire_Id(classeId, anneeId)
                .stream()
                .map(this::mapInscription)
                .collect(Collectors.toList());
    }

    public List<InscriptionResponseDTO> getByClasseEtAnnee(Long classeId, Long anneeId) {

        return inscriptionRepository
                .findByClasseIdAndAnneeScolaire_Id(classeId, anneeId)
                .stream()
                .filter(i -> i.getStatut() == StatutInscription.VALIDE)
                .map(i -> {

                    InscriptionResponseDTO dto = mapInscription(i);

                    System.out.println(
                            "INSCRIPTION ID = " + dto.getId()
                                    + " | ELEVE = " + dto.getNom()
                    );

                    return dto;
                })
                .collect(Collectors.toList());
    }

    public InscriptionResponseDTO getById(Long id) {

        Inscription inscription = inscriptionRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Inscription introuvable")
                );

        return mapInscription(inscription);
    }
    // =========================
    // 👨‍🎓 ÉLÈVES ACTIFS
    // =========================

    public List<EleveResponseDTO> getElevesByEcoleAndAnneeActive(Long ecoleId) {

        AnneeScolaire anneeActive = anneeScolaireRepository
                .findByEcoleIdAndActiveTrue(ecoleId)
                .orElseThrow(() -> new RuntimeException("Aucune année active"));

        return inscriptionRepository
                .findByEcoleIdAndAnneeScolaire_Id(ecoleId, anneeActive.getId())
                .stream()
                .map(this::mapEleve)
                .toList();
    }

    @Transactional
    public Inscription validerInscription(Long inscriptionId) {

        Inscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        if (inscription.getStatut() == StatutInscription.VALIDE) {
            throw new RuntimeException("Cette inscription est déjà validée");
        }

        inscription.setStatut(StatutInscription.VALIDE);
        return inscriptionRepository.save(inscription);
    }

    @Transactional
    public Inscription rejeterInscription(Long inscriptionId) {

        Inscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        if (inscription.getStatut() == StatutInscription.REFUSE) {
            throw new RuntimeException("Cette inscription est déjà refusée");
        }

        inscription.setStatut(StatutInscription.REFUSE);
        return inscriptionRepository.save(inscription);
    }

    // =========================
    // 📋 TOUTES INSCRIPTIONS
    // =========================
    public List<InscriptionResponseDTO> getInscriptions() {
        return inscriptionRepository.findAll()
                .stream()
                .map(this::mapInscription)
                .toList();
    }

    // =========================
    // 👨‍🎓 TOUS ÉLÈVES
    // =========================
    public List<EleveResponseDTO> getAllEleves() {
        return inscriptionRepository.findAll()
                .stream()
                .map(this::mapEleve)
                .toList();
    }

    // =========================
    // 💰 AGRÉGATION DES LIGNES DE FRAIS (helper commun)
    // =========================
    private record MontantsAgreges(double total, double paye, double reste, String statutPaiement) {}

    private MontantsAgreges agregerMontants(Long inscriptionId) {

        List<LigneFrais> lignes = ligneFraisRepository.findByInscriptionId(inscriptionId);

        double total = lignes.stream().mapToDouble(LigneFrais::getMontantTotal).sum();
        double paye = lignes.stream().mapToDouble(LigneFrais::getMontantPaye).sum();
        double reste = total - paye;

        String statut = reste <= 0 ? "PAYE" : (paye > 0 ? "PARTIEL" : "NON_PAYE");

        return new MontantsAgreges(total, paye, reste, statut);
    }

    // =========================
    // 🔁 MAPPING INSCRIPTION
    // =========================
    private InscriptionResponseDTO mapInscription(Inscription i) {

        Eleve e = i.getEleve();

        InscriptionResponseDTO dto = new InscriptionResponseDTO();

        dto.setId(i.getId());

        dto.setNom(e.getNom());
        dto.setPrenom(e.getPrenom());
        dto.setMatricule(e.getMatricule());
        dto.setSexe(e.getSexe());
        dto.setDateNaissance(e.getDateNaissance());
        dto.setLieuNaissance(e.getLieuNaissance());
        dto.setNationalite(e.getNationalite());
        dto.setGroupeSanguin(e.getGroupeSanguin());
        dto.setAllergiesMaladies(e.getAllergiesMaladies());

        dto.setAdresse(e.getAdresse());
        dto.setTelephone(e.getTelephone());
        dto.setEmail(e.getEmail());

        dto.setNomTuteur(e.getNomTuteur());
        dto.setPrenomTuteur(e.getPrenomTuteur());
        dto.setLienParente(e.getLienParente());
        dto.setTelephoneTuteur(e.getTelephoneTuteur());
        dto.setEmailTuteur(e.getEmailTuteur());

        dto.setClasseNom(i.getClasse().getNomComplet());
        dto.setAnnee(i.getAnneeScolaire().getNom());
        dto.setDateInscription(i.getCreatedAt());
        dto.setStatut(i.getStatut().name());

        MontantsAgreges montants = agregerMontants(i.getId());
        dto.setMontantTotal(montants.total());
        dto.setMontantPaye(montants.paye());
        dto.setResteAPayer(montants.reste());
        dto.setStatutPaiement(montants.statutPaiement());

        dto.setEcoleId(i.getEcole().getId());

        return dto;
    }
    @Transactional
    public Inscription modifierInscription(
            Long id,
            InscriptionDTO request
    ) {

        Inscription inscription = inscriptionRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Inscription introuvable")
                );

        Eleve eleve = inscription.getEleve();

        eleve.setNom(request.getNom());
        eleve.setPrenom(request.getPrenom());
        eleve.setDateNaissance(request.getDateNaissance());
        eleve.setSexe(request.getSexe());
        eleve.setLieuNaissance(request.getLieuNaissance());
        eleve.setNationalite(request.getNationalite());
        eleve.setNumeroExtraitNaissance(request.getNumeroExtraitNaissance());
        eleve.setGroupeSanguin(request.getGroupeSanguin());
        eleve.setAllergiesMaladies(request.getAllergiesMaladies());

        eleve.setAdresse(request.getAdresse());
        eleve.setTelephone(request.getTelephone());
        eleve.setEmail(request.getEmail());

        eleve.setNomTuteur(request.getNomTuteur());
        eleve.setPrenomTuteur(request.getPrenomTuteur());
        eleve.setLienParente(request.getLienParente());
        eleve.setTelephoneTuteur(request.getTelephoneTuteur());
        eleve.setEmailTuteur(request.getEmailTuteur());
        eleve.setProfessionTuteur(request.getProfessionTuteur());
        eleve.setAdresseTuteur(request.getAdresseTuteur());

        eleveRepository.save(eleve);


        if(request.getClasseId()!=null){

            Classe classe = classeRepository.findById(request.getClasseId())
                    .orElseThrow(() ->
                            new RuntimeException("Classe introuvable")
                    );

            inscription.setClasse(classe);
        }


        return inscriptionRepository.save(inscription);
    }

    private ReinscriptionReponseDTO mapReinscription(Inscription i) {

        ReinscriptionReponseDTO dto = new ReinscriptionReponseDTO();

        dto.setId(i.getId());
        dto.setNom(i.getEleve().getNom());
        dto.setPrenom(i.getEleve().getPrenom());
        dto.setMatricule(i.getEleve().getMatricule());
        dto.setSexe(i.getEleve().getSexe());
        dto.setClasseNom(i.getClasse().getNomComplet());
        dto.setAnnee(i.getAnneeScolaire().getNom());
        dto.setDateInscription(i.getCreatedAt());

        dto.setStatut(i.getStatut().name());

        MontantsAgreges montants = agregerMontants(i.getId());
        dto.setMontantTotal(montants.total());
        dto.setMontantPaye(montants.paye());
        dto.setResteAPayer(montants.reste());
        dto.setStatutPaiement(montants.statutPaiement());

        dto.setDateNaissance(i.getEleve().getDateNaissance());

        Double moyenne = noteService.calculMoyenneAnnuelle(
                i.getEleve().getId(),

                i.getAnneeScolaire().getId()
        );

        dto.setMoyenneAnnuelle(moyenne);
        dto.setMention(calculerMention(moyenne));
        dto.setDecision(decisionDepuisMoyenne(moyenne));

        return dto;
    }

    private String calculerMention(Double moyenne) {
        if (moyenne == null) return "-";
        if (moyenne >= 16) return "Très Bien";
        if (moyenne >= 14) return "Bien";
        if (moyenne >= 12) return "Assez Bien";
        if (moyenne >= 10) return "Passable";
        return "Ajourné";
    }

    private String decisionDepuisMoyenne(Double moyenne) {
        return (moyenne != null && moyenne >= 10) ? "ADMIS" : "REDOUBLANT";
    }

    // =========================
    // 🔁 MAPPING ELEVE
    // =========================
    private EleveResponseDTO mapEleve(Inscription i) {

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

        dto.setStatut(i.getStatut().name());
        dto.setDateInscription(i.getCreatedAt().toString());

        return dto;
    }

    // =========================
    // 🔁 RÉINSCRIPTION
    // =========================
    @Transactional
    public void reinscrire(Long inscriptionId, Long nouvelleClasseId) {

        Inscription ancienneInscription = inscriptionRepository
                .findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        Long ecoleId = ancienneInscription.getEcole().getId();

        AnneeScolaire anneeActive = anneeScolaireRepository
                .findByEcoleIdAndActiveTrue(ecoleId)
                .orElseThrow(() -> new RuntimeException("Année active introuvable"));

        boolean existeDeja = inscriptionRepository.existsByEleveIdAndAnneeScolaireId(
                ancienneInscription.getEleve().getId(),
                anneeActive.getId()
        );

        if (existeDeja) {
            throw new RuntimeException("Cet élève est déjà réinscrit pour cette année");
        }

        Classe nouvelleClasse = classeRepository
                .findById(nouvelleClasseId)
                .orElseThrow(() -> new RuntimeException("Classe introuvable"));

        Inscription nouvelleInscription = new Inscription();
        nouvelleInscription.setEleve(ancienneInscription.getEleve());
        nouvelleInscription.setClasse(nouvelleClasse);
        nouvelleInscription.setEcole(ancienneInscription.getEcole());
        nouvelleInscription.setAnneeScolaire(anneeActive);
        nouvelleInscription.setCreatedAt(LocalDateTime.now());
        nouvelleInscription.setStatut(StatutInscription.PREINSCRIT);

        Inscription savedInscription = inscriptionRepository.save(nouvelleInscription);
        // 🔥 Génère une ligne de frais par type (INSCRIPTION, SCOLARITE, EXAMEN, UNIFORME...)
        ligneFraisService.genererLignesFrais(savedInscription);


    }

    public List<ReinscriptionReponseDTO> getElevesPourReinscription(Long ecoleId) {

        AnneeScolaire anneeActive = anneeScolaireRepository
                .findByEcoleIdAndActiveTrue(ecoleId)
                .orElseThrow(() -> new RuntimeException("Aucune année active"));

        AnneeScolaire anneePrecedente = anneeScolaireRepository
                .findTopByEcoleIdAndDateFinBeforeOrderByDateFinDesc(
                        ecoleId,
                        anneeActive.getDateDebut()
                )
                .orElse(null);

        if (anneePrecedente == null) {
            return Collections.emptyList();
        }

        return inscriptionRepository
                .findByEcoleIdAndAnneeScolaireId(ecoleId, anneePrecedente.getId())
                .stream()
                .map(inscriptionPrecedente -> {

                    Long eleveId = inscriptionPrecedente.getEleve().getId();

                    Inscription inscriptionActive = inscriptionRepository
                            .findByEleveIdAndAnneeScolaireId(eleveId, anneeActive.getId())
                            .orElse(null);

                    ReinscriptionReponseDTO dto;

                    if (inscriptionActive != null) {
                        dto = mapReinscription(inscriptionActive);
                        dto.setClasseNom(inscriptionPrecedente.getClasse().getNomComplet());
                        dto.setStatutReinscription("REINSCRIT");
                        dto.setNouvelleClasseNom(inscriptionActive.getClasse().getNomComplet());
                    } else {
                        dto = mapReinscription(inscriptionPrecedente);
                        dto.setStatutReinscription("NON_REINSCRIT");
                    }

                    Double moyenne = noteService.calculMoyenneAnnuelle(
                            eleveId,

                            anneePrecedente.getId()

                    );
                    System.out.println(moyenne);

                    dto.setMoyenneAnnuelle(moyenne);
                    dto.setMention(calculerMention(moyenne));
                    dto.setDecision(decisionDepuisMoyenne(moyenne));

                    return dto;
                })
                .toList();
    }

    public List<InscriptionResponseDTO> getElevesPrReinscription(Long ecoleId) {

        AnneeScolaire anneeActive = anneeScolaireRepository
                .findByEcoleIdAndActiveTrue(ecoleId)
                .orElseThrow(() -> new RuntimeException("Aucune année active"));

        AnneeScolaire anneePrecedente = anneeScolaireRepository
                .findTopByEcoleIdAndDateFinBeforeOrderByDateFinDesc(
                        ecoleId,
                        anneeActive.getDateDebut()
                )
                .orElse(null);

        if (anneePrecedente == null) {
            return List.of();
        }

        return inscriptionRepository
                .findByEcoleIdAndAnneeScolaireId(ecoleId, anneePrecedente.getId())
                .stream()
                .map(inscriptionPrecedente -> {

                    Long eleveId = inscriptionPrecedente.getEleve().getId();

                    Inscription inscriptionActive = inscriptionRepository
                            .findByEleveIdAndAnneeScolaireId(eleveId, anneeActive.getId())
                            .orElse(null);

                    InscriptionResponseDTO dto;

                    if (inscriptionActive != null) {
                        dto = mapInscription(inscriptionActive);
                        dto.setStatut("REINSCRIT");
                    } else {
                        dto = mapInscription(inscriptionPrecedente);
                        dto.setStatut("NON_REINSCRIT");
                    }

                    return dto;
                })
                .toList();
    }

    public List<EleveDTO> getElevesClasseActive(Long classeId) {

        return inscriptionRepository
                .findElevesActifsClasse(classeId)
                .stream()
                .map(inscription -> {

                    Eleve e = inscription.getEleve();

                    EleveDTO dto = new EleveDTO();

                    dto.setId(e.getId());
                    dto.setInscriptionId(inscription.getId());

                    dto.setNom(e.getNom());
                    dto.setPrenom(e.getPrenom());

                    return dto;
                })
                .toList();
    }

    // =========================
    // 🔧 HELPERS PRIVÉS
    // =========================

    private Role obtenirOuCreerRoleEleve(Ecole ecole) {

        Optional<Role> roleExistant =
                roleRepository.findByNomAndEcoleId(
                        ROLE_ELEVE,
                        ecole.getId()
                );

        if(roleExistant.isPresent()) {
            return roleExistant.get();
        }


        Role nouveauRole = new Role();

        nouveauRole.setNom(ROLE_ELEVE);
        nouveauRole.setEcole(ecole);

        return roleRepository.save(nouveauRole);
    }

    private Utilisateur creerCompteUtilisateurEleve(String nom, String prenom, Ecole ecole) {

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(nom);
        utilisateur.setEmail(genererEmailUnique(nom, prenom));
        utilisateur.setPassword(passwordEncoder.encode(genererMotDePasseTemporaire()));
        utilisateur.setRole(obtenirOuCreerRoleEleve(ecole));
        utilisateur.setEcole(ecole);

        return utilisateurRepository.save(utilisateur);
    }

    private String genererEmailUnique(String nom, String prenom) {
        String base = normaliser(prenom) + "." + normaliser(nom);
        String suffixe = UUID.randomUUID().toString().substring(0, 6);
        return base + "." + suffixe + "@" + DOMAINE_COMPTE_ELEVE;
    }

    private String normaliser(String valeur) {
        String sansAccents = Normalizer.normalize(valeur, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sansAccents.trim().toLowerCase().replaceAll("[^a-z0-9]+", "");
    }

    private String genererMotDePasseTemporaire() {
        return UUID.randomUUID().toString().substring(0, 10);
    }
}
package com.saas.school.service;

import com.saas.school.dto.LoginRequest;
import com.saas.school.dto.RegisterRequest;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EcoleRepository ecoleRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final AbonnementService abonnementService;
    private final AnneeScolaireService anneeScolaireService;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TypeFraisRepository typeFraisRepository;
    private final JwtService jwtService;
    private final CycleRepository cycleRepository;

    // ⚠️ NOUVEAUX repositories (à adapter à tes noms réels)
    private final NiveauRepository niveauRepository;
    private final SerieRepository serieRepository;
    private final GroupeRepository groupeRepository;
    private final SalleRepository salleRepository;

    @Transactional
    public void register(RegisterRequest request) {

        // ============================================================
        // 0. VALIDATION ANNÉE SCOLAIRE
        // ============================================================

        LocalDate debut = request.getDateDebutAnneeScolaire();
        LocalDate fin = request.getDateFinAnneeScolaire();

        if (debut == null || fin == null) {
            throw new RuntimeException(
                    "Les dates de début et de fin de l'année scolaire sont obligatoires."
            );
        }

        if (!fin.isAfter(debut)) {
            throw new RuntimeException(
                    "La date de fin doit être postérieure à la date de début."
            );
        }

        // Nom automatique : 2026-2027
        String nom = debut.getYear() + "-" + fin.getYear();

        // ============================================================
        // 1. CRÉER ÉCOLE + DONNÉES PAR DÉFAUT
        // ============================================================

        Ecole ecole = new Ecole();

        ecole.setNom(request.getNomEcole());
        ecole.setAdresse(request.getAdresse());
        ecole.setVille(request.getVille());
        ecole.setPays(request.getPays());
        ecole.setTelephone(request.getTelephone());

        ecole.setCreatedAt(LocalDateTime.now());
        ecole.setActive(true);

        ecoleRepository.save(ecole);

        // Les niveaux ont besoin des cycles : on récupère la map nom -> Cycle
        Map<String, Cycle> cycles = creerCyclesParDefaut(ecole);
        Map<String, Niveau> niveaux = creerNiveauxParDefaut(ecole, cycles);
        creerSeriesParDefaut(ecole, niveaux);
        creerGroupesParDefaut(ecole);
        creerSallesParDefaut(ecole);

        // ============================================================
        // 2. ASSIGNER PLAN BASIC
        // ============================================================

        abonnementService.assignerPlan(
                ecole.getId(),
                PlanAbonnement.BASIC,
                1
        );

        // ============================================================
        // 3. CRÉER LES RÔLES
        // ============================================================

        Role role = creerRolesParDefaut(ecole);

        // ============================================================
        // 4. CRÉER ADMIN
        // ============================================================

        Utilisateur user = new Utilisateur();

        user.setEmail(request.getEmail());
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setRole(role);
        user.setEcole(ecole);

        utilisateurRepository.save(user);

        // ============================================================
        // 5. CRÉER L'ANNÉE SCOLAIRE
        // ============================================================

        AnneeScolaire as = anneeScolaireService.creer(
                nom,
                debut,
                fin,
                ecole.getId()
        );

        // Activer automatiquement l'année créée
        anneeScolaireService.activer(as.getId());

        // ============================================================
        // 6. CRÉER LES TYPES DE FRAIS PAR DÉFAUT
        // ============================================================

        creerTypesFraisParDefaut(ecole);
    }

    public Map<String, Object> login(LoginRequest request) {

        Utilisateur user = utilisateurRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Email incorrect")
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        Ecole ecole = user.getEcole();

        // SUPER ADMIN bypass
        boolean isSuperAdmin =
                user.getRole() != null &&
                        "SUPER_ADMIN".equals(user.getRole().getNom());

        if (!isSuperAdmin) {

            if (ecole == null) {
                throw new RuntimeException("École introuvable");
            }

            if (!ecole.isActive()) {
                throw new RuntimeException("École désactivée");
            }

            if (ecole.getDateFin() != null &&
                    ecole.getDateFin().isBefore(LocalDate.now())) {

                throw new RuntimeException("Abonnement expiré");
            }
        }

        // ============================================================
        // PERMISSIONS
        // ============================================================

        List<String> permissions = user.getRole()
                .getPermissions()
                .stream()
                .map(Permission::getCode)
                .toList();

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().getNom(),
                permissions
        );

        // ============================================================
        // RESPONSE
        // ============================================================

        Map<String, Object> response =
                new java.util.HashMap<>();

        response.put("token", token);
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("role", user.getRole().getNom());
        response.put("roleId", user.getRole().getId());
        response.put("permissions", permissions);

        if (ecole != null) {

            Map<String, Object> ecoleMap =
                    new java.util.HashMap<>();

            ecoleMap.put("id", ecole.getId());
            ecoleMap.put("nom", ecole.getNom());
            ecoleMap.put("plan", ecole.getPlan());
            ecoleMap.put("dateFin", ecole.getDateFin());
            ecoleMap.put("logo", ecole.getLogo());

            response.put("ecole", ecoleMap);
        }

        return response;
    }

    // ============================================================
    // CRÉATION DES RÔLES
    // ============================================================

    private Role creerRolesParDefaut(Ecole ecole) {

        Role adminRole = null;

        List<String> roles = List.of(
                "ADMIN",
                "DIRECTEUR",
                "ENSEIGNANT",
                "COMPTABLE",
                "SECRETAIRE"
        );

        for (String nomRole : roles) {

            Role role = roleRepository
                    .findByNomAndEcole(nomRole, ecole)
                    .orElseGet(() -> {

                        Role r = new Role();

                        r.setNom(nomRole);
                        r.setEcole(ecole);

                        return roleRepository.save(r);
                    });

            if ("ADMIN".equals(nomRole)) {
                adminRole = role;
            }
        }

        return adminRole;
    }

    // ============================================================
    // TYPES DE FRAIS
    // ============================================================

    private void creerTypesFraisParDefaut(Ecole ecole) {

        Map<String, FrequenceFrais> typesAvecFrequence =
                Map.of(
                        "INSCRIPTION", FrequenceFrais.UNIQUE,
                        "SCOLARITE", FrequenceFrais.ANNUEL,
                        "EXAMEN", FrequenceFrais.UNIQUE,
                        "UNIFORME", FrequenceFrais.UNIQUE
                );

        for (Map.Entry<String, FrequenceFrais> entry
                : typesAvecFrequence.entrySet()) {

            String code = entry.getKey();
            FrequenceFrais frequence = entry.getValue();

            boolean existe = typeFraisRepository
                    .findByEcoleIdAndCode(
                            ecole.getId(),
                            code
                    )
                    .isPresent();

            if (!existe) {

                TypeFrais tf = new TypeFrais();

                tf.setCode(code);
                tf.setLibelle(code);
                tf.setFrequence(frequence);
                tf.setEcole(ecole);

                typeFraisRepository.save(tf);
            }
        }
    }

    // ============================================================
    // PERMISSIONS
    // ============================================================

    public void assignerPermissionsRole(
            Long roleId,
            List<String> codes
    ) {

        Role role = roleRepository
                .findById(roleId)
                .orElseThrow();

        List<Permission> perms =
                permissionRepository.findAllByCodeIn(codes);

        role.setPermissions(perms);

        roleRepository.save(role);
    }

    // ============================================================
    // CYCLES PAR DÉFAUT (retourne la map nom -> Cycle)
    // ============================================================

    private Map<String, Cycle> creerCyclesParDefaut(Ecole ecole) {

        List<String> noms = List.of(
                "PREMIER CYCLE",
                "SECOND CYCLE",
                "LYCEE",
                "PROFESSIONNEL"
        );

        Map<String, Cycle> resultat = new LinkedHashMap<>();

        for (String nomCycle : noms) {

            Cycle cycle = cycleRepository
                    .findByNomAndEcole(nomCycle, ecole)
                    .orElseGet(() -> {
                        Cycle c = new Cycle();
                        c.setNom(nomCycle);
                        c.setEcole(ecole);
                        return cycleRepository.save(c);
                    });

            resultat.put(nomCycle, cycle);
        }

        return resultat;
    }

    // ============================================================
    // NIVEAUX PAR DÉFAUT (retourne la map "CYCLE|NIVEAU" -> Niveau)
    // ============================================================

    private Map<String, Niveau> creerNiveauxParDefaut(Ecole ecole, Map<String, Cycle> cycles) {

        // cycle -> niveaux (modifie cette liste selon ton système scolaire)
        Map<String, List<String>> niveauxParCycle = new LinkedHashMap<>();

        niveauxParCycle.put("PREMIER CYCLE", List.of(
                "1ère", "2ème", "3ème",
                "4ème", "5ème", "6ème"
        ));

        niveauxParCycle.put("SECOND CYCLE", List.of(
                "7ème", "8ème", "9ème"
        ));

        niveauxParCycle.put("LYCEE", List.of(
                "10ème", "11ème", "12ème"
        ));

        // Mêmes noms que le premier cycle : c'est le couple (cycle, niveau) qui identifie la classe
        niveauxParCycle.put("PROFESSIONNEL", List.of(
                "1ERE ANNEE", "2EME ANNEE", "3EME ANNEE"
        ));

        List<Niveau> existants = niveauRepository.findByEcoleId(ecole.getId());
        Map<String, Niveau> resultat = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : niveauxParCycle.entrySet()) {

            String nomCycle = entry.getKey();
            Cycle cycle = cycles.get(nomCycle);

            for (String nomNiveau : entry.getValue()) {

                // Le nom d'un niveau n'est unique que DANS SON CYCLE
                Niveau niveau = existants.stream()
                        .filter(n -> n.getCycle() != null
                                && n.getCycle().getId().equals(cycle.getId())
                                && n.getNom() != null
                                && n.getNom().equalsIgnoreCase(nomNiveau))
                        .findFirst()
                        .orElseGet(() -> {
                            Niveau nv = new Niveau();
                            nv.setNom(nomNiveau);
                            nv.setCycle(cycle);
                            nv.setEcole(ecole);
                            return niveauRepository.save(nv);
                        });

                resultat.put(nomCycle + "|" + nomNiveau, niveau);
            }
        }

        return resultat;
    }

    // ============================================================
    // SÉRIES PAR DÉFAUT (rattachées à un niveau)
    // ============================================================

    /** Séries proposées par défaut pour un niveau (modifie selon ton programme). */
    private List<String> seriesPourNiveau(String nomCycle, String nomNiveau) {

        if ("LYCEE".equals(nomCycle)) {
            if ("10ème".equals(nomNiveau)) {
                return List.of("CG");
            }
            else if("11ème".equals(nomNiveau)){
                return List.of( "SCIENCES",
                        "LETTRES");
            }
            return List.of(
                    "TSE",    // Sciences exactes
                    "TSEXP",  // Sciences expérimentales
                    "TSECO",  // Sciences économiques
                    "TSS",    // Sciences sociales
                    "TLL"


            );
        } else if ("PROFESSIONNEL".equals(nomCycle)) {
            return List.of(
                    "DB",
                    "EM",
                    "ELECT"



            );
        }

        // Tous les autres niveaux : une série neutre, car la série est obligatoire
        return List.of("ANNEE");
    }

    private void creerSeriesParDefaut(Ecole ecole, Map<String, Niveau> niveaux) {

        for (Map.Entry<String, Niveau> entry : niveaux.entrySet()) {

            String[] cle = entry.getKey().split("\\|");
            Niveau niveau = entry.getValue();

            for (String nomSerie : seriesPourNiveau(cle[0], cle[1])) {

                boolean existe = serieRepository
                        .existsByNomAndNiveauId(nomSerie, niveau.getId());

                if (!existe) {
                    Serie serie = new Serie();
                    serie.setNom(nomSerie);
                    serie.setNiveau(niveau);
                    serie.setEcole(ecole);
                    serieRepository.save(serie);
                }
            }
        }
    }

    // ============================================================
    // GROUPES PAR DÉFAUT
    // ============================================================

    private void creerGroupesParDefaut(Ecole ecole) {

        List<String> groupes = List.of("GROUPE A", "GROUPE B", "GROUPE C");

        for (String nomGroupe : groupes) {

            boolean existe = groupeRepository
                    .existsByNomAndEcoleId(nomGroupe, ecole.getId());

            if (!existe) {
                Groupe groupe = new Groupe();
                groupe.setNom(nomGroupe);
                groupe.setEcole(ecole);
                groupeRepository.save(groupe);
            }
        }
    }

    // ============================================================
    // SALLES PAR DÉFAUT
    // ============================================================

    private void creerSallesParDefaut(Ecole ecole) {

        List<String> salles = List.of(
                "SALLE 1", "SALLE 2", "SALLE 3", "SALLE 4", "SALLE 5"
        );

        for (String nomSalle : salles) {

            boolean existe = salleRepository
                    .existsByNomAndEcoleId(nomSalle, ecole.getId());

            if (!existe) {
                Salle salle = new Salle();
                salle.setNom(nomSalle);
                salle.setEcole(ecole);
                salle.setActive(true);
                salleRepository.save(salle);
            }
        }
    }
}
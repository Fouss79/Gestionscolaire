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
        // 1. CRÉER ÉCOLE
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

        System.out.println(
                "PERMISSIONS FROM DB = " +
                        user.getRole()
                                .getPermissions()
                                .stream()
                                .map(Permission::getCode)
                                .toList()
        );

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
}
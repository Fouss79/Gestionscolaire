package com.saas.school.service;

import com.saas.school.dto.LoginRequest;
import com.saas.school.dto.RegisterRequest;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
    private final  JwtService jwtService;
    public void register(RegisterRequest request) {

        // 1. créer école
        Ecole ecole = new Ecole();
        ecole.setNom(request.getNomEcole());
        ecole.setAdresse(request.getAdresse());
        ecole.setVille(request.getVille());
        ecole.setPays(request.getPays());
        ecole.setCreatedAt(LocalDateTime.now());
        ecole.setActive(true);
        ecole.setTelephone(request.getTelephone());

        ecoleRepository.save(ecole);

        // 🔥 2. assigner plan BASIC automatiquement
        abonnementService.assignerPlan(
                ecole.getId(),
                PlanAbonnement.BASIC,
                1 // 1 mois gratuit ou 1 mois d'essai
        );

        // 3. créer admin
        Utilisateur user = new Utilisateur();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));


        Role role =creerRolesParDefaut(ecole);




        user.setRole(role);
        user.setEcole(ecole);

        utilisateurRepository.save(user);
        LocalDate debut = LocalDate.parse("2025-10-01");
        LocalDate fin = LocalDate.parse("2026-07-31");
        AnneeScolaire as = anneeScolaireService.creer("2025-2026",debut,fin,ecole.getId());
        anneeScolaireService.activer(as.getId());
        creerTypesFraisParDefaut(ecole);

    }
    public Map<String, Object> login(LoginRequest request) {

        Utilisateur user = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email incorrect"));


        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        Ecole ecole = user.getEcole();

        // 🔐 SUPER ADMIN bypass
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
                    ecole.getDateFin().isBefore(java.time.LocalDate.now())) {
                throw new RuntimeException("Abonnement expiré");
            }
        }

        // 🔥 permissions du rôle
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

        // 📦 RESPONSE
        Map<String, Object> response = new java.util.HashMap<>();

        response.put("token", token);
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("role", user.getRole().getNom());
        response.put("roleId", user.getRole().getId());
        response.put("permissions", permissions);

        if (ecole != null) {
            Map<String, Object> ecoleMap = new java.util.HashMap<>();

            ecoleMap.put("id", ecole.getId());
            ecoleMap.put("nom", ecole.getNom());
            ecoleMap.put("plan", ecole.getPlan());
            ecoleMap.put("dateFin", ecole.getDateFin());

            // 🖼️ Logo de l'école
            ecoleMap.put("logo", ecole.getLogo());

            response.put("ecole", ecoleMap);
        }
        System.out.println("PERMISSIONS FROM DB = " +
                user.getRole().getPermissions()
                        .stream()
                        .map(Permission::getCode)
                        .toList()
        );
        return response;
    }

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

            Role role = roleRepository.findByNomAndEcole(nomRole, ecole)
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
    private void creerTypesFraisParDefaut(Ecole ecole) {

        Map<String, FrequenceFrais> typesAvecFrequence = Map.of(
                "INSCRIPTION", FrequenceFrais.UNIQUE,
                "SCOLARITE", FrequenceFrais.MENSUEL,
                "EXAMEN", FrequenceFrais.TRIMESTRIEL,
                "UNIFORME", FrequenceFrais.UNIQUE
        );

        for (Map.Entry<String, FrequenceFrais> entry : typesAvecFrequence.entrySet()) {

            String code = entry.getKey();
            FrequenceFrais frequence = entry.getValue();

            boolean existe = typeFraisRepository
                    .findByEcoleIdAndCode(ecole.getId(), code)
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
    public void assignerPermissionsRole(Long roleId, List<String> codes) {
        Role role = roleRepository.findById(roleId).orElseThrow();


        List<Permission> perms = permissionRepository.findAllByCodeIn(codes);

        role.setPermissions(perms);

        roleRepository.save(role);
    }
}


package com.saas.school.controller;

import com.saas.school.dto.ChangerRoleRequest;
import com.saas.school.entity.Role;
import com.saas.school.entity.Utilisateur;
import com.saas.school.repository.RoleRepository;
import com.saas.school.repository.UtilisateurRepository;
import com.saas.school.service.SupabaseStorageService;
import com.saas.school.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UtilisateurController {

    // ===== DTO (ajoutés : ils manquaient et empêchaient la compilation) =====
    public record ProfilRequest(String nom) {}

    public record PasswordRequest(String ancienMotDePasse, String nouveauMotDePasse) {}

    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurService utilisateurService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SupabaseStorageService supabaseStorageService;

    // ============================================================
    // OUTILS
    // ============================================================

    private Utilisateur utilisateurConnecte(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof Utilisateur principal) {
            // On recharge depuis la base pour avoir des données à jour
            return utilisateurRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
    }

    private Long ecoleId(Utilisateur u) {
        return u.getEcole() != null ? u.getEcole().getId() : null;
    }

    /** Vérifie que l'utilisateur connecté est ADMIN de l'école indiquée. */
    private void verifierAdminDeLEcole(Utilisateur connecte, Long ecoleId) {
        boolean admin = connecte.getRole() != null
                && "ADMIN".equalsIgnoreCase(connecte.getRole().getNom());

        boolean memeEcole = ecoleId != null
                && ecoleId.equals(ecoleId(connecte));

        if (!admin || !memeEcole) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé.");
        }
    }

    /** Charge l'utilisateur ciblé après avoir vérifié les droits de l'admin connecté. */
    private Utilisateur utilisateurGere(Long id, Authentication auth) {
        Utilisateur cible = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        verifierAdminDeLEcole(utilisateurConnecte(auth), ecoleId(cible));
        return cible;
    }

    // ============================================================
    // ADMIN : gestion des utilisateurs de son école
    // ============================================================

    @GetMapping
    public List<Utilisateur> getAll(Authentication auth) {
        // Uniquement les utilisateurs de l'école de l'utilisateur connecté
        return utilisateurRepository.findByEcoleId(ecoleId(utilisateurConnecte(auth)));
    }

    @GetMapping("/ecole/{ecoleId}/utilisateurs")
    public List<Utilisateur> getUtilisateurs(@PathVariable Long ecoleId, Authentication auth) {
        verifierAdminDeLEcole(utilisateurConnecte(auth), ecoleId);
        return utilisateurRepository.findByEcoleId(ecoleId);
    }

    @GetMapping("/roles")
    public List<Role> getRoles() {
        return roleRepository.findAll();
    }

    @PutMapping("/changer-role")
    public Utilisateur changerRole(@RequestBody ChangerRoleRequest request, Authentication auth) {
        // Vérifie les droits avant de déléguer au service
        utilisateurGere(request.getUtilisateurId(), auth);

        return utilisateurService.changerRole(
                request.getUtilisateurId(),
                request.getRoleId()
        );
    }

    @PutMapping("/{id}")
    public Utilisateur modifier(@PathVariable Long id,
                                @RequestBody Utilisateur data,
                                Authentication auth) {

        Utilisateur u = utilisateurGere(id, auth);

        if (data.getNom() == null || data.getNom().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire.");
        }
        if (data.getEmail() == null || data.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'email est obligatoire.");
        }

        String email = data.getEmail().trim();

        // Email déjà utilisé par quelqu'un d'autre ?
        utilisateurRepository.findByEmail(email).ifPresent(autre -> {
            if (!autre.getId().equals(id)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Cet email est déjà utilisé.");
            }
        });

        u.setNom(data.getNom().trim());
        u.setEmail(email);

        if (data.getPassword() != null && !data.getPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(data.getPassword()));
            u.setMotDePasseTemporaire(null);
        }

        return utilisateurRepository.save(u);
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Utilisateur uploadPhoto(@PathVariable Long id,
                                   @RequestParam("file") MultipartFile file,
                                   Authentication auth) {

        Utilisateur u = utilisateurGere(id, auth);

        u.setPhoto(supabaseStorageService.uploadImage(file, "users"));
        return utilisateurRepository.save(u);
    }

    @GetMapping("/{id}")
    public Utilisateur getById(@PathVariable Long id, Authentication auth) {
        return utilisateurGere(id, auth);
    }

    // ============================================================
    // PROFIL : l'utilisateur connecté modifie son propre compte
    // ============================================================

    @GetMapping("/me")
    public Utilisateur me(Authentication auth) {
        return utilisateurConnecte(auth);
    }

    @PutMapping("/me")
    public Utilisateur modifierProfil(Authentication auth, @RequestBody ProfilRequest req) {
        if (req.nom() == null || req.nom().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire.");
        }

        Utilisateur u = utilisateurConnecte(auth);
        u.setNom(req.nom().trim());
        return utilisateurRepository.save(u);
    }

    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Utilisateur modifierPhoto(Authentication auth,
                                     @RequestParam("file") MultipartFile file) {

        Utilisateur u = utilisateurConnecte(auth);
        u.setPhoto(supabaseStorageService.uploadImage(file, "users"));
        return utilisateurRepository.save(u);
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changerMotDePasse(Authentication auth,
                                                  @RequestBody PasswordRequest req) {

        Utilisateur u = utilisateurConnecte(auth);

        if (req.ancienMotDePasse() == null
                || !passwordEncoder.matches(req.ancienMotDePasse(), u.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Le mot de passe actuel est incorrect.");
        }

        if (req.nouveauMotDePasse() == null || req.nouveauMotDePasse().length() < 6) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le nouveau mot de passe doit contenir au moins 6 caractères.");
        }

        u.setPassword(passwordEncoder.encode(req.nouveauMotDePasse()));
        u.setMotDePasseTemporaire(null);
        utilisateurRepository.save(u);

        return ResponseEntity.ok().build();
    }
}
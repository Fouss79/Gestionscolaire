package com.saas.school;

import com.saas.school.entity.*;
import com.saas.school.repository.EcoleRepository;
import com.saas.school.repository.PermissionRepository;
import com.saas.school.repository.RoleRepository;
import com.saas.school.repository.UtilisateurRepository;
import com.saas.school.service.AnneeScolaireService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@AllArgsConstructor
@SpringBootApplication
public class SchoolApplication {
	@Autowired
private final EcoleRepository ecoleRepository;
	private final AnneeScolaireService anneeScolaireService;
	private PermissionRepository permissionRepository;



	public static void main(String[] args) {
		SpringApplication.run(SchoolApplication.class, args);
	}

	@Bean
	CommandLineRunner initData(
			UtilisateurRepository userRepository,
			PasswordEncoder passwordEncoder,
			RoleRepository roleRepository,
			PermissionRepository permissionRepository,
			AnneeScolaireService anneeScolaireService
	) {
		return args -> {

			// Permissions
			creerPermission(permissionRepository,
					"GESTION_MATIERES",
					"Gestion des matieres");
			creerPermission(permissionRepository,
					"GESTION_ELEVES",
					"Gestion des élèves");

			creerPermission(permissionRepository,
					"GESTION_CLASSES",
					"Gestion des classes");

			creerPermission(permissionRepository,
					"GESTION_NOTES",
					"Gestion des notes");

			creerPermission(permissionRepository,
					"GESTION_ENSEIGNANTS",
					"Gestion des enseignants");

			creerPermission(permissionRepository,
					"GESTION_PAIEMENTS",
					"Gestion des paiements");

			creerPermission(permissionRepository,
					"GESTION_UTILISATEURS",
					"Gestion des utilisateurs");

			creerPermission(permissionRepository,
					"GESTION_ROLES",
					"Gestion des rôles");
			creerPermission(permissionRepository,
					"GESTION_EMARGEMENTS",
					"Gestion des emargements");
			creerPermission(permissionRepository,
					"GESTION_PRESENCES",
					"Gestion des presences");
			creerPermission(permissionRepository,
					"GESTION_EMPLOIS_DU_TEMPS",
					"Gestion des emplois du temps");




			// Rôles
			Role superAdminRole = creerRole(roleRepository, "SUPER_ADMIN");


			// Super Admin
			if (userRepository.findByEmail("admin@gmail.com").isEmpty()) {

				Utilisateur admin = new Utilisateur();
				admin.setNom("Admin");
				admin.setEmail("admin@gmail.com");
				admin.setPassword(passwordEncoder.encode("admin123"));
				admin.setRole(superAdminRole);

				Ecole ecole = new Ecole();
				ecole.setActive(true);

				ecole = ecoleRepository.save(ecole);
				LocalDate debut = LocalDate.parse("2025-10-01");
				LocalDate fin = LocalDate.parse("2026-07-31");
				AnneeScolaire as = anneeScolaireService.creer("2025-2026",debut,fin,ecole.getId());

				anneeScolaireService.activer(as.getId());


				admin.setEcole(ecole);

				userRepository.save(admin);

				System.out.println("✅ SuperAdmin créé !");
			}
		};
	}
	private Role creerRole(RoleRepository roleRepository, String nom) {

		Role role = roleRepository.findByNom(nom);

		if (role == null) {
			role = new Role();
			role.setNom(nom);
			role = roleRepository.save(role);
		}

		return role;
	}
	private void creerPermission(
			PermissionRepository permissionRepository,
			String code,
			String description
	) {

		if (!permissionRepository.existsByCode(code)) {

			Permission permission = new Permission();
			permission.setCode(code);
			permission.setDescription(description);

			permissionRepository.save(permission);
		}
	}
}

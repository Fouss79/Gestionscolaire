package com.saas.school.service;

import com.saas.school.entity.Inscription;
import com.saas.school.entity.Note;
import com.saas.school.repository.InscriptionRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BulletinMailService {

    private final InscriptionRepository inscriptionRepository;
    private final BulletinService bulletinService;
    private final JavaMailSender mailSender;

    // =========================================================
    // 📧 ENVOI POUR UN SEUL ÉLÈVE
    // =========================================================

    public void envoyerBulletinParent(
            Long inscriptionId,
            Long classeId,
            Long anneeScolaireId,
            String periode
    ) {

        Inscription inscription = inscriptionRepository
                .findById(inscriptionId)
                .orElseThrow(() ->
                        new RuntimeException("Inscription introuvable")
                );

        // ⚠️ À VÉRIFIER : on suppose que l'email du tuteur est un champ
        // direct de l'entité Inscription (comme sur InscriptionResponseDTO).
        // Si en réalité il vient d'une relation (ex: inscription.getTuteur()),
        // adapte cette ligne en conséquence.
        String emailTuteur = inscription.getEleve().getEmailTuteur();

        if (emailTuteur == null || emailTuteur.isBlank()) {
            throw new RuntimeException(
                    "Aucune adresse email de tuteur enregistrée pour cet élève."
            );
        }

        // Même logique que la génération PDF individuelle : matières
        // affectées à la classe + notes déjà saisies pour la période.
        List<Note> notes = bulletinService.construireNotesPourBulletin(
                inscription,
                classeId,
                anneeScolaireId,
                periode
        );

        // Pas de classement calculé ici (nécessiterait les moyennes de
        // toute la classe, méthode privée dans BulletinService) : le
        // bulletin envoyé par email affichera "Rang : -".
        byte[] pdf = bulletinService.generateBulletin(
                notes,
                inscription.getEleve(),
                periode,
                inscription,
                null
        );

        String nomEleve =
                inscription.getEleve().getPrenom()
                        + " "
                        + inscription.getEleve().getNom();

        envoyerEmailAvecPieceJointe(
                emailTuteur,
                "Bulletin scolaire - " + nomEleve + " - " + periode,
                "Bonjour,\n\n"
                        + "Veuillez trouver ci-joint le bulletin scolaire de "
                        + nomEleve
                        + " pour la période \""
                        + periode
                        + "\".\n\n"
                        + "Cordialement.",
                pdf,
                "bulletin-" + inscription.getEleve().getNom() + ".pdf"
        );
    }

    // =========================================================
    // 📧 ENVOI GROUPÉ POUR TOUTE UNE CLASSE
    // =========================================================

    public void envoyerBulletinsClasse(
            Long classeId,
            Long anneeScolaireId,
            String periode
    ) {

        // Réutilise la même sélection d'élèves que
        // BulletinService.generateBulletinsClasse().
        List<Inscription> inscriptions =
                inscriptionRepository.findElevesValidesPourReleve(
                        classeId,
                        anneeScolaireId
                );

        if (inscriptions == null || inscriptions.isEmpty()) {
            throw new RuntimeException(
                    "Aucun élève valide dans cette classe pour cette année scolaire."
            );
        }

        int envoyes = 0;
        int echoues = 0;

        for (Inscription inscription : inscriptions) {
            try {
                envoyerBulletinParent(
                        inscription.getId(),
                        classeId,
                        anneeScolaireId,
                        periode
                );
                envoyes++;
            } catch (Exception e) {
                // On continue l'envoi aux autres élèves même si un envoi
                // individuel échoue (email manquant, erreur SMTP, etc.).
                echoues++;
                System.out.println(
                        "⚠️ Échec envoi bulletin inscription #"
                                + inscription.getId() + " : " + e.getMessage()
                );
            }
        }

        if (envoyes == 0) {
            throw new RuntimeException(
                    "Aucun bulletin n'a pu être envoyé (" + echoues + " échec(s))."
            );
        }
    }

    // =========================================================
    // ✉️ ENVOI TECHNIQUE DE L'EMAIL AVEC PIÈCE JOINTE
    // =========================================================

    private void envoyerEmailAvecPieceJointe(
            String destinataire,
            String sujet,
            String corps,
            byte[] pieceJointe,
            String nomFichier
    ) {

        try {

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true);

            helper.setTo(destinataire);
            helper.setSubject(sujet);
            helper.setText(corps);

            helper.addAttachment(
                    nomFichier,
                    new ByteArrayResource(pieceJointe)
            );

            mailSender.send(message);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erreur lors de l'envoi de l'email : "
                            + e.getMessage(),
                    e
            );
        }
    }
}
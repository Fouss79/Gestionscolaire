package com.saas.school.controller;

import com.saas.school.service.BulletinMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bulletins")
@RequiredArgsConstructor
public class BulletinMailController {

    private final BulletinMailService bulletinMailService;

    // =========================================================
    // 📧 ENVOYER LE BULLETIN D'UN ÉLÈVE À SON/SES PARENT(S)
    // =========================================================

    @PostMapping("/envoyer-parent")
    public ResponseEntity<?> envoyerAuParent(
            @RequestParam Long inscriptionId,
            @RequestParam Long classeId,
            @RequestParam Long anneeId,
            @RequestParam String periode
    ) {

        try {

            bulletinMailService.envoyerBulletinParent(
                    inscriptionId,
                    classeId,
                    anneeId,
                    periode
            );

            return ResponseEntity.ok().build();

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .badRequest()
                    .body(java.util.Map.of(
                            "message", e.getMessage()
                    ));
        }
    }

    // =========================================================
    // 📧 ENVOYER LES BULLETINS DE TOUTE UNE CLASSE
    // =========================================================

    @PostMapping("/envoyer-parent-classe")
    public ResponseEntity<?> envoyerALaClasse(
            @RequestParam Long classeId,
            @RequestParam Long anneeId,
            @RequestParam String periode
    ) {

        try {

            bulletinMailService.envoyerBulletinsClasse(
                    classeId,
                    anneeId,
                    periode
            );

            return ResponseEntity.ok().build();

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .badRequest()
                    .body(java.util.Map.of(
                            "message", e.getMessage()
                    ));
        }
    }
}
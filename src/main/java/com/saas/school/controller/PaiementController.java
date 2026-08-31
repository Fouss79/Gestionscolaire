package com.saas.school.controller;

import com.saas.school.dto.PaiementRequestDTO;
import com.saas.school.dto.PaiementResponseDTO;
import com.saas.school.entity.Paiement;
import com.saas.school.entity.PaiementScolarite;
import com.saas.school.entity.PlanAbonnement;
import com.saas.school.repository.PaiementRepository;
import com.saas.school.service.AbonnementService;
import com.saas.school.service.PaiementService;
import com.saas.school.service.RecuService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.view.RedirectView;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/paiements")
@RequiredArgsConstructor
public class PaiementController {

    private final AbonnementService abonnementService;
    private final PaiementService paiementService;
    private final PaiementRepository paiementRepository;
    private final RecuService recuService;
    @PostMapping("/init")
    public Map<String, Object> init(
            @RequestParam Long ecoleId,
            @RequestParam PlanAbonnement plan,
            @RequestParam int duree
    ) {

        Paiement paiement = paiementService.creerPaiement(ecoleId, plan, duree);

        // 🔥 ici tu remplaces par PayDunya API
        String paymentUrl = "http://localhost:8080/api/paiements/fake-payment?id=" + paiement.getId();

        return Map.of(
                "url", paymentUrl,
                "paiementId", paiement.getId()
        );
    }
    @PostMapping
    public ResponseEntity<PaiementResponseDTO> payer(
            @RequestBody PaiementRequestDTO dto) {

        return ResponseEntity.ok(
                paiementService.enregistrerPaiement(dto)
        );
    }

    @GetMapping("/fake-payment")
    public RedirectView fakePayment(@RequestParam Long id) {

        Paiement p = paiementRepository.findById(id).orElseThrow();

        // simuler callback
        abonnementService.assignerPlan(
                p.getEcoleId(),
                p.getPlan(),
                p.getDuree()
        );

        p.setStatus("SUCCESS");
        paiementRepository.save(p);

        return new RedirectView("http://localhost:3000/dashboard/admin/monabonnement");
    }
    @PostMapping("/callback")
    public String callback(@RequestBody Map<String, Object> data) {

        try {

            String status = data.get("status").toString();
            Long paiementId = Long.valueOf(data.get("paiementId").toString());

            if (!status.equals("success")) {
                return "Paiement échoué";
            }

            Paiement p = paiementRepository.findById(paiementId).orElseThrow();

            // 🔥 sécuriser (éviter fraude)
            if (!p.getStatus().equals("PENDING")) {
                return "Déjà traité";
            }

            // 🔥 activer abonnement
            abonnementService.assignerPlan(
                    p.getEcoleId(),
                    p.getPlan(),
                    p.getDuree()
            );

            p.setStatus("SUCCESS");
            paiementRepository.save(p);

            return "OK";

        } catch (Exception e) {
            return "ERROR";
        }
    }

    @GetMapping("/inscription/{inscriptionId}")
    public List<PaiementResponseDTO> getByInscription(@PathVariable Long inscriptionId) {
        return paiementService.getByInscription(inscriptionId);
    }
    @GetMapping("/ecole/{ecoleId}")
    public List<PaiementResponseDTO> getByEcole(@PathVariable Long ecoleId) {
        return paiementService.getByEcole(ecoleId);
    }
    @GetMapping("/{paiementId}/recus")
    public ResponseEntity<byte[]> genererRecu(@PathVariable Long paiementId) {

        byte[] pdf = recuService.genererRecuPdf(paiementId);

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_PDF);

        headers.setContentDisposition(
                ContentDisposition.builder("attachment")
                        .filename("recu-paiement-" + paiementId + ".pdf")
                        .build()
        );

        headers.setContentLength(pdf.length);

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(pdf);
    }
}
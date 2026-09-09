package com.saas.school.controller;

import com.saas.school.service.BulletinSalaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/paiements")
@RequiredArgsConstructor
@CrossOrigin("*")
public class BulletinSalaireController {

    private final BulletinSalaireService bulletinSalaireService;

    @GetMapping("/{id}/bulletin")
    public ResponseEntity<byte[]> getBulletin(@PathVariable Long id) {

        byte[] pdf = bulletinSalaireService.genererBulletinPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("bulletin-salaire-" + id + ".pdf").build()
        );

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
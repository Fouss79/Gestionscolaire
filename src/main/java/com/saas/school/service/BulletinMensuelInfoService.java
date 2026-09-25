package com.saas.school.service;

import com.saas.school.dto.InfoDto;
import com.saas.school.dto.InfosRequest;
import com.saas.school.entity.BulletinMensuelInfo;
import com.saas.school.entity.Inscription;
import com.saas.school.repository.BulletinMensuelInfoRepository;
import com.saas.school.repository.InscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BulletinMensuelInfoService {

    private final BulletinMensuelInfoRepository infoRepo;
    private final InscriptionRepository inscriptionRepo;

    @Transactional(readOnly = true)
    public List<InfoDto> lister(Long classeId, Long anneeId, String mois) {
        List<Inscription> inscriptions =  inscriptionRepo.findActifsByClasseAndAnnee(
                classeId,
                anneeId,
                StatutInscription.VALIDE
        );
        List<Long> ids = inscriptions.stream().map(Inscription::getId).toList();

        Map<Long, BulletinMensuelInfo> existantes = ids.isEmpty()
                ? Map.of()
                : infoRepo.findByInscriptionIdInAndMois(ids, mois).stream()
                        .collect(Collectors.toMap(i -> i.getInscription().getId(), i -> i));

        return inscriptions.stream().map(ins -> {
            BulletinMensuelInfo info = existantes.get(ins.getId());
            return new InfoDto(
                    ins.getId(),
                    info != null ? info.getAbsences() : 0,
                    info != null && info.getObservationMaitre() != null ? info.getObservationMaitre() : "");
        }).toList();
    }

    @Transactional
    public void enregistrer(InfosRequest request) {
        for (InfoDto dto : request.infos()) {
            Inscription ins = inscriptionRepo.findById(dto.inscriptionId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Inscription introuvable : " + dto.inscriptionId()));

            // TODO sécurité : vérifier que "ins" appartient à l'école de l'utilisateur connecté,
            // comme sur vos autres endpoints.

            BulletinMensuelInfo info = infoRepo
                    .findByInscriptionIdAndMois(ins.getId(), request.mois())
                    .orElseGet(() -> {
                        BulletinMensuelInfo nouveau = new BulletinMensuelInfo();
                        nouveau.setInscription(ins);
                        nouveau.setMois(request.mois());
                        return nouveau;
                    });

            info.setAbsences(dto.absences() == null ? 0 : dto.absences());
            info.setObservationMaitre(dto.observationMaitre() == null ? "" : dto.observationMaitre().trim());
            infoRepo.save(info);
        }
    }
}

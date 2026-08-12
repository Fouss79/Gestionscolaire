package com.saas.school.repository;

import com.saas.school.entity.Presence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

    Optional<Presence> findByInscriptionIdAndEmploiDuTempsIdAndDate(
            Long inscriptionId, Long edtId, LocalDate date
    );

    List<Presence> findByEmploiDuTempsIdAndDate(Long edtId, LocalDate date);

    List<Presence> findByInscription_Classe_IdAndDate(Long classeId, LocalDate date);

    long countByInscriptionIdAndPeriodeIdAndStatut(
            Long inscriptionId, Long periodeId, Presence.StatutPresence statut
    );
}
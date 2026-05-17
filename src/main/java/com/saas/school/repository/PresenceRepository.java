package com.saas.school.repository;

import com.saas.school.entity.Presence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

    Optional<Presence> findByEleveIdAndEmploiDuTempsId(Long eleveId, Long edtId);


    Optional<Presence> findByEleveIdAndEmploiDuTempsIdAndDate(
            Long eleveId, Long edtId, LocalDate date
    );

    void deleteByEmploiDuTempsClasseIdAndDate(Long classeId, LocalDate date);


    List<Presence> findByEmploiDuTempsIdAndDate(Long edtId, LocalDate date);
    // 🔥 NOUVELLE MÉTHODE POUR STATS
    List<Presence> findByEleveClasseId(Long classeId);


    List<Presence> findByEmploiDuTempsId(Long edtId);
}
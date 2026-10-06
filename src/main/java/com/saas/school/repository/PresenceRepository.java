package com.saas.school.repository;

import com.saas.school.entity.Presence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Arrays;
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

    List<Presence> findByInscription_Classe_IdAndDateBetween(Long classeId, LocalDate debut, LocalDate fin);

    List<Presence> findByInscriptionIdAndDateBetweenOrderByDateAsc(Long inscriptionId, LocalDate debut, LocalDate fin);

    // PresenceRepository
    long countByInscriptionIdAndStatut(Long inscriptionId, Presence.StatutPresence statut);

    @Query("select count(distinct p.date) from Presence p " +
            "where p.inscription.id = :inscriptionId and p.statut = :statut")
    long countJoursByInscriptionAndStatut(@Param("inscriptionId") Long inscriptionId,
                                          @Param("statut") Presence.StatutPresence statut);}
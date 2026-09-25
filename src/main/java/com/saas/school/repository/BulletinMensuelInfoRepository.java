package com.saas.school.repository;

import com.saas.school.entity.BulletinMensuelInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BulletinMensuelInfoRepository extends JpaRepository<BulletinMensuelInfo, Long> {

    Optional<BulletinMensuelInfo> findByInscriptionIdAndMois(Long inscriptionId, String mois);

    List<BulletinMensuelInfo> findByInscriptionIdInAndMois(Collection<Long> inscriptionIds, String mois);
}

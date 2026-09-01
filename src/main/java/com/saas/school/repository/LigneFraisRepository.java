package com.saas.school.repository;

import com.saas.school.entity.LigneFrais;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LigneFraisRepository extends JpaRepository<LigneFrais, Long> {

    List<LigneFrais> findByInscriptionId(Long inscriptionId);

    List<LigneFrais> findByInscription_Ecole_IdAndInscription_AnneeScolaire_ActiveTrue(Long ecoleId);

    Optional<LigneFrais> findByInscriptionIdAndTypeFrais_Code(Long inscriptionId, String codeTypeFrais);

    // Une seule ligne par (inscription, typeFrais) — utilisé à la génération
    // pour éviter les doublons, quelle que soit la fréquence (ANNUEL ou UNIQUE).
    boolean existsByInscriptionIdAndTypeFraisId(Long inscriptionId, Long typeFraisId);

    // Toutes les lignes estimatives d'un niveau/année/type, pour recalcul
    List<LigneFrais> findByInscription_Classe_Niveau_IdAndInscription_AnneeScolaire_IdAndTypeFrais_CodeAndEstimatifTrue(
            Long niveauId, Long anneeScolaireId, String codeTypeFrais
    );

    List<LigneFrais> findByInscription_Classe_Niveau_IdAndInscription_AnneeScolaire_IdAndTypeFrais_Code(
            Long niveauId, Long anneeScolaireId, String codeTypeFrais
    );
    List<LigneFrais> findByInscription_Id( Long inscriptionId );

}
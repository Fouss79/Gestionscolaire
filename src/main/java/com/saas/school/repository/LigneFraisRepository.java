package com.saas.school.repository;

import com.saas.school.entity.LigneFrais;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LigneFraisRepository extends JpaRepository<LigneFrais, Long> {

    List<LigneFrais> findByInscriptionId(Long inscriptionId);

    List<LigneFrais> findByInscription_Ecole_IdAndInscription_AnneeScolaire_ActiveTrue(Long ecoleId);

    Optional<LigneFrais> findByInscriptionIdAndTypeFrais_Code(Long inscriptionId, String codeTypeFrais);

    Optional<LigneFrais> findByInscriptionIdAndTypeFrais_CodeAndMois(
            Long inscriptionId, String codeTypeFrais, Integer mois
    );

    Optional<LigneFrais> findByInscriptionIdAndTypeFrais_CodeAndMoisIsNull(
            Long inscriptionId, String codeTypeFrais
    );

    boolean existsByInscriptionIdAndTypeFraisIdAndMoisIsNull(Long inscriptionId, Long typeFraisId);

    boolean existsByInscriptionIdAndTypeFraisIdAndMoisAndAnnee(
            Long inscriptionId, Long typeFraisId, Integer mois, Integer annee
    );

    // ⚠️ nouveau — toutes les lignes estimatives d'un niveau/année/type, pour recalcul
    List<LigneFrais> findByInscription_Classe_Niveau_IdAndInscription_AnneeScolaire_IdAndTypeFrais_CodeAndEstimatifTrue(
            Long niveauId, Long anneeScolaireId, String codeTypeFrais
    );

    List<LigneFrais> findByInscription_Classe_Niveau_IdAndInscription_AnneeScolaire_IdAndTypeFrais_Code(Long niveauId, Long anneeScolaireId, String codeTypeFrais);
}
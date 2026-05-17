package com.saas.school.repository;



import com.saas.school.entity.Ecole;
import com.saas.school.entity.PlanAbonnement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EcoleRepository extends JpaRepository<Ecole, Long> {
    long countByPlan(PlanAbonnement plan);
    long countByActiveTrue();

}

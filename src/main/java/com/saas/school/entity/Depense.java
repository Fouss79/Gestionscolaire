package com.saas.school.entity;

import com.saas.school.service.StatutPaiement;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class Depense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String libelle;

    private String description;

    @Column(nullable = false)
    private LocalDate dateDepense;

    // Montant total de la dépense (ex : facture fournisseur de 500 000 FCFA)
    @Column(nullable = false)
    private Double montantTotal;

    // Somme de tous les PaiementDepense déjà enregistrés sur cette dépense
    @Column(nullable = false)
    private Double montantPaye = 0.0;

    // montantTotal - montantPaye
    @Column(nullable = false)
    private Double resteAPayer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPaiement statutPaiement = StatutPaiement.NON_PAYE;

    @ManyToOne
    @JoinColumn(name = "categorie_id")
    private CategorieDepense categorie;

    @ManyToOne
    @JoinColumn(name = "ecole_id", nullable = false)
    private Ecole ecole;
}
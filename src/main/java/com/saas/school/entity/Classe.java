package com.saas.school.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_classe_niveau_serie_groupe_ecole",
                columnNames = {"niveau_id", "serie_id", "groupe_id", "ecole_id"}
        )
})

public class Classe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnoreProperties({"classes"})
    @ManyToOne
    private Niveau niveau;

    @ManyToOne
    private Serie serie;

    @ManyToOne(optional = true)
    @JoinColumn(name = "groupe_id", nullable = true)
    private Groupe groupe;

    @ManyToOne
    @JoinColumn(name = "ecole_id")
    private Ecole ecole;
    public String getNomComplet() {
        return String.join(" ",
                niveau != null ? niveau.getNom() : "",
                serie != null ? serie.getNom() : "",
                groupe != null ? groupe.getNom() : ""
        ).trim().replaceAll("\\s+", " ");
    }

    public String getNomNiveau() {
        return niveau != null ? niveau.getNom() : "";
    }
    @ManyToOne
    @JoinColumn(name = "salle_id")
    private Salle salle;
}

package com.saas.school.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Classe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnoreProperties({"classes"})
    @ManyToOne
    private Niveau niveau;

    @ManyToOne
    private Serie serie;

    @ManyToOne
    private Groupe groupe;

    @ManyToOne
    @JoinColumn(name = "ecole_id")
    private Ecole ecole;
    public String getNomComplet() {
        return niveau.getNom() + " " + serie.getNom() + " " + groupe.getNom();
    }

    public String getNomNiveau() {
        return niveau != null ? niveau.getNom() : "";
    }
}

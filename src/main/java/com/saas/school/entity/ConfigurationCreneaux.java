package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(
        name = "configuration_creneaux",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"ecole_id", "cycle_id"}
        )
)
public class ConfigurationCreneaux {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ecole_id", nullable = false)
    private Ecole ecole;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cycle_id", nullable = false)
    private Cycle cycle;

    @OneToMany(
            mappedBy = "configuration",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("jour ASC, ordre ASC")
    private List<CreneauHoraire> creneaux = new ArrayList<>();
}
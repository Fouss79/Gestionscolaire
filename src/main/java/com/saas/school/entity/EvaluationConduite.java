package com.saas.school.entity;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "evaluation_conduite",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_conduite_inscription_periode",
                        columnNames = {"inscription_id", "periode"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EvaluationConduite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscription_id", nullable = false)
    private Inscription inscription;

    @Column(nullable = false, length = 80)
    private String periode;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal note;

    @Column(nullable = false)
    private Integer coefficient = 1;

    @Column(length = 500)
    private String appreciation;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        coefficient = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        coefficient = 1;
    }
}

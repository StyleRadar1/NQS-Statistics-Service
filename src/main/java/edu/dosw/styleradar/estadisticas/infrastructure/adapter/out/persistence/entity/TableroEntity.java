package edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity;

import edu.dosw.styleradar.estadisticas.domain.model.TipoTablero;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tableros")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableroEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "id_dueno", nullable = false)
    private UUID idDueño;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoTablero tipo;

    @Builder.Default
    @OneToMany(mappedBy = "tablero", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<IndicadorEntity> indicadores = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "tablero", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<GraficaEntity> graficas = new ArrayList<>();
}

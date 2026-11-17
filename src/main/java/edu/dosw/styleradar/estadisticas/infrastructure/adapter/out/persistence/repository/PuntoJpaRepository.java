package edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository;

import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.PuntoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PuntoJpaRepository extends JpaRepository<PuntoEntity, UUID> {

    @Query("SELECT p FROM PuntoEntity p WHERE p.serie.id = :serieId AND p.fecha >= :inicio AND p.fecha <= :fin ORDER BY p.fecha ASC")
    List<PuntoEntity> findBySerieIdAndPeriodo(
            @Param("serieId") UUID serieId,
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin
    );
}

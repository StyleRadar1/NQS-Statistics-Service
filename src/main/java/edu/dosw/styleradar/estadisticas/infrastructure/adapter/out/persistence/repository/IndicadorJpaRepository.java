package edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository;

import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.IndicadorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface IndicadorJpaRepository extends JpaRepository<IndicadorEntity, UUID> {

    @Query("SELECT i FROM IndicadorEntity i WHERE i.tablero.id = :tableroId " +
           "AND i.periodoInicio >= :inicio AND i.periodoFin <= :fin")
    List<IndicadorEntity> findByTableroIdAndPeriodo(
            @Param("tableroId") UUID tableroId,
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin
    );
}

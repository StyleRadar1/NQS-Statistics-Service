package edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository;

import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.GraficaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GraficaJpaRepository extends JpaRepository<GraficaEntity, UUID> {
    Optional<GraficaEntity> findByTableroIdAndTitulo(UUID tableroId, String titulo);
    List<GraficaEntity> findByTableroId(UUID tableroId);
}

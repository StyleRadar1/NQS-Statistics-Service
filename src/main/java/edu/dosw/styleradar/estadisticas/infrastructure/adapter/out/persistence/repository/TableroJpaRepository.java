package edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository;

import edu.dosw.styleradar.estadisticas.domain.model.TipoTablero;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.TableroEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TableroJpaRepository extends JpaRepository<TableroEntity, UUID> {
    Optional<TableroEntity> findByTipoAndIdDueño(TipoTablero tipo, UUID idDueño);
}

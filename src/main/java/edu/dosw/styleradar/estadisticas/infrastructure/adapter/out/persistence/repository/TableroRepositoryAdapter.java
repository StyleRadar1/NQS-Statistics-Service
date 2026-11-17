package edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository;

import edu.dosw.styleradar.estadisticas.domain.model.Comparacion;
import edu.dosw.styleradar.estadisticas.domain.model.Grafica;
import edu.dosw.styleradar.estadisticas.domain.model.Indicador;
import edu.dosw.styleradar.estadisticas.domain.model.Periodo;
import edu.dosw.styleradar.estadisticas.domain.model.Punto;
import edu.dosw.styleradar.estadisticas.domain.model.Serie;
import edu.dosw.styleradar.estadisticas.domain.model.Tablero;
import edu.dosw.styleradar.estadisticas.domain.model.TipoTablero;
import edu.dosw.styleradar.estadisticas.domain.model.UnidadMedida;
import edu.dosw.styleradar.estadisticas.domain.port.out.ConsultarTemporalidadPort;
import edu.dosw.styleradar.estadisticas.domain.port.out.TableroRepositoryPort;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.GraficaEntity;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.IndicadorEntity;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.PuntoEntity;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.SerieEntity;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.TableroEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TableroRepositoryAdapter implements TableroRepositoryPort, ConsultarTemporalidadPort {

    private final TableroJpaRepository tableroJpaRepository;
    private final IndicadorJpaRepository indicadorJpaRepository;
    private final GraficaJpaRepository graficaJpaRepository;
    private final PuntoJpaRepository puntoJpaRepository;

    @Autowired
    public TableroRepositoryAdapter(
            TableroJpaRepository tableroJpaRepository,
            IndicadorJpaRepository indicadorJpaRepository,
            GraficaJpaRepository graficaJpaRepository,
            PuntoJpaRepository puntoJpaRepository
    ) {
        this.tableroJpaRepository = tableroJpaRepository;
        this.indicadorJpaRepository = indicadorJpaRepository;
        this.graficaJpaRepository = graficaJpaRepository;
        this.puntoJpaRepository = puntoJpaRepository;
    }

    public TableroRepositoryAdapter(TableroJpaRepository tableroJpaRepository, IndicadorJpaRepository indicadorJpaRepository) {
        this(tableroJpaRepository, indicadorJpaRepository, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tablero> findByTipoAndIdDueño(TipoTablero tipo, UUID idDueño) {
        return tableroJpaRepository.findByTipoAndIdDueño(tipo, idDueño)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public Tablero save(Tablero tablero) {
        TableroEntity entity = toEntity(tablero);
        TableroEntity savedEntity = tableroJpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Indicador> findIndicadoresByTableroIdAndPeriodo(UUID tableroId, LocalDate fechaInicio, LocalDate fechaFin) {
        List<IndicadorEntity> entities = indicadorJpaRepository.findByTableroIdAndPeriodo(tableroId, fechaInicio, fechaFin);
        return entities.stream().map(this::toDomainIndicador).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Grafica> findGraficaById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return graficaJpaRepository.findById(id).map(this::toDomainGrafica);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Grafica> findGraficaByTableroIdAndTitulo(UUID tableroId, String titulo) {
        if (tableroId == null || titulo == null) {
            return Optional.empty();
        }
        return graficaJpaRepository.findByTableroIdAndTitulo(tableroId, titulo).map(this::toDomainGrafica);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tablero> findTableroByTipoAndIdDueño(TipoTablero tipo, UUID idDueño) {
        return findByTipoAndIdDueño(tipo, idDueño);
    }

    @Override
    @Transactional
    public Grafica saveGrafica(Grafica grafica, UUID tableroId) {
        if (grafica == null || tableroId == null) {
            throw new IllegalArgumentException("La gráfica y el id del tablero son requeridos");
        }
        TableroEntity tableroEntity = tableroJpaRepository.findById(tableroId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el tablero con id: " + tableroId));
        GraficaEntity entity = toEntityGrafica(grafica, tableroEntity);
        GraficaEntity saved = graficaJpaRepository.save(entity);
        return toDomainGrafica(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Punto> findPuntosBySerieIdAndPeriodo(UUID serieId, LocalDate fechaInicio, LocalDate fechaFin) {
        if (serieId == null || fechaInicio == null || fechaFin == null) {
            return Collections.emptyList();
        }
        return puntoJpaRepository.findBySerieIdAndPeriodo(serieId, fechaInicio, fechaFin)
                .stream().map(this::toDomainPunto).toList();
    }

    public Tablero toDomain(TableroEntity entity) {
        if (entity == null) {
            return null;
        }

        List<Indicador> indicadores = entity.getIndicadores() != null
                ? entity.getIndicadores().stream().map(this::toDomainIndicador).toList()
                : Collections.emptyList();

        List<Grafica> graficas = entity.getGraficas() != null
                ? entity.getGraficas().stream().map(this::toDomainGrafica).toList()
                : Collections.emptyList();

        return Tablero.builder()
                .id(entity.getId())
                .idDueño(entity.getIdDueño())
                .tipo(entity.getTipo())
                .indicadores(new ArrayList<>(indicadores))
                .graficas(new ArrayList<>(graficas))
                .build();
    }

    public Indicador toDomainIndicador(IndicadorEntity entity) {
        if (entity == null) {
            return null;
        }

        Periodo periodo = null;
        if (entity.getPeriodoInicio() != null && entity.getPeriodoFin() != null) {
            periodo = new Periodo(entity.getPeriodoInicio(), entity.getPeriodoFin());
        }

        Comparacion comparacion = new Comparacion(entity.getValorAnterior(), entity.getVariacionPorcentual());

        UnidadMedida unidad = null;
        if (entity.getUnidad() != null) {
            try {
                unidad = UnidadMedida.valueOf(entity.getUnidad());
            } catch (IllegalArgumentException e) {
                unidad = UnidadMedida.CANTIDAD;
            }
        }

        return Indicador.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .valor(entity.getValor())
                .unidad(unidad)
                .fecha(entity.getFecha())
                .periodo(periodo)
                .comparacion(comparacion)
                .build();
    }

    public Grafica toDomainGrafica(GraficaEntity entity) {
        if (entity == null) {
            return null;
        }

        List<Serie> series = entity.getSeries() != null
                ? entity.getSeries().stream().map(this::toDomainSerie).toList()
                : Collections.emptyList();

        return Grafica.builder()
                .id(entity.getId())
                .titulo(entity.getTitulo())
                .series(new ArrayList<>(series))
                .build();
    }

    public Serie toDomainSerie(SerieEntity entity) {
        if (entity == null) {
            return null;
        }

        List<Punto> puntos = entity.getPuntos() != null
                ? entity.getPuntos().stream().map(this::toDomainPunto).toList()
                : Collections.emptyList();

        return Serie.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .tipo(entity.getTipo())
                .puntos(new ArrayList<>(puntos))
                .build();
    }

    public Punto toDomainPunto(PuntoEntity entity) {
        if (entity == null) {
            return null;
        }
        return Punto.builder()
                .id(entity.getId())
                .fecha(entity.getFecha())
                .valor(entity.getValor())
                .build();
    }

    public TableroEntity toEntity(Tablero domain) {
        if (domain == null) {
            return null;
        }

        TableroEntity entity = TableroEntity.builder()
                .id(domain.getId() != null ? domain.getId() : UUID.randomUUID())
                .idDueño(domain.getIdDueño())
                .tipo(domain.getTipo())
                .build();

        if (domain.getIndicadores() != null) {
            List<IndicadorEntity> indicadorEntities = domain.getIndicadores().stream()
                    .map(i -> toEntityIndicador(i, entity))
                    .toList();
            entity.setIndicadores(new ArrayList<>(indicadorEntities));
        }

        if (domain.getGraficas() != null) {
            List<GraficaEntity> graficaEntities = domain.getGraficas().stream()
                    .map(g -> toEntityGrafica(g, entity))
                    .toList();
            entity.setGraficas(new ArrayList<>(graficaEntities));
        }

        return entity;
    }

    public IndicadorEntity toEntityIndicador(Indicador domain, TableroEntity tableroEntity) {
        if (domain == null) {
            return null;
        }

        return IndicadorEntity.builder()
                .id(domain.getId() != null ? domain.getId() : UUID.randomUUID())
                .tablero(tableroEntity)
                .nombre(domain.getNombre())
                .valor(domain.getValor())
                .unidad(domain.getUnidad() != null ? domain.getUnidad().name() : UnidadMedida.CANTIDAD.name())
                .fecha(domain.getFecha())
                .periodoInicio(domain.getPeriodo() != null ? domain.getPeriodo().getInicio() : null)
                .periodoFin(domain.getPeriodo() != null ? domain.getPeriodo().getFin() : null)
                .valorAnterior(domain.getComparacion() != null ? domain.getComparacion().getValorAnterior() : null)
                .variacionPorcentual(domain.getComparacion() != null ? domain.getComparacion().getVariacionPorcentual() : null)
                .build();
    }

    public GraficaEntity toEntityGrafica(Grafica domain, TableroEntity tableroEntity) {
        if (domain == null) {
            return null;
        }

        GraficaEntity entity = GraficaEntity.builder()
                .id(domain.getId() != null ? domain.getId() : UUID.randomUUID())
                .tablero(tableroEntity)
                .titulo(domain.getTitulo())
                .build();

        if (domain.getSeries() != null) {
            List<SerieEntity> serieEntities = domain.getSeries().stream()
                    .map(s -> toEntitySerie(s, entity))
                    .toList();
            entity.setSeries(new ArrayList<>(serieEntities));
        }

        return entity;
    }

    public SerieEntity toEntitySerie(Serie domain, GraficaEntity graficaEntity) {
        if (domain == null) {
            return null;
        }

        SerieEntity entity = SerieEntity.builder()
                .id(domain.getId() != null ? domain.getId() : UUID.randomUUID())
                .grafica(graficaEntity)
                .nombre(domain.getNombre())
                .tipo(domain.getTipo())
                .build();

        if (domain.getPuntos() != null) {
            List<PuntoEntity> puntoEntities = domain.getPuntos().stream()
                    .map(p -> toEntityPunto(p, entity))
                    .toList();
            entity.setPuntos(new ArrayList<>(puntoEntities));
        }

        return entity;
    }

    public PuntoEntity toEntityPunto(Punto domain, SerieEntity serieEntity) {
        if (domain == null) {
            return null;
        }

        return PuntoEntity.builder()
                .id(domain.getId() != null ? domain.getId() : UUID.randomUUID())
                .serie(serieEntity)
                .fecha(domain.getFecha())
                .valor(domain.getValor())
                .build();
    }
}

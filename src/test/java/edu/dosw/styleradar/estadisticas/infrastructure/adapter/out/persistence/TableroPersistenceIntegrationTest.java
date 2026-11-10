package edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence;

import edu.dosw.styleradar.estadisticas.domain.model.Comparacion;
import edu.dosw.styleradar.estadisticas.domain.model.Grafica;
import edu.dosw.styleradar.estadisticas.domain.model.Indicador;
import edu.dosw.styleradar.estadisticas.domain.model.Periodo;
import edu.dosw.styleradar.estadisticas.domain.model.Punto;
import edu.dosw.styleradar.estadisticas.domain.model.Serie;
import edu.dosw.styleradar.estadisticas.domain.model.Tablero;
import edu.dosw.styleradar.estadisticas.domain.model.TipoTablero;
import edu.dosw.styleradar.estadisticas.domain.model.UnidadMedida;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.IndicadorEntity;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.TableroEntity;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.IndicadorJpaRepository;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.TableroJpaRepository;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.TableroRepositoryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class TableroPersistenceIntegrationTest {

    @Autowired
    private TableroJpaRepository tableroJpaRepository;

    @Autowired
    private IndicadorJpaRepository indicadorJpaRepository;

    private TableroRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TableroRepositoryAdapter(tableroJpaRepository, indicadorJpaRepository);
    }

    @Test
    @DisplayName("Integración · persistencia: Debe guardar y recuperar tablero con indicadores vía adapter")
    void testGuardarYRecuperarTableroViaAdapter() {
        UUID idDueño = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 10, 1);
        LocalDate fin = LocalDate.of(2026, 10, 31);
        Periodo periodo = new Periodo(inicio, fin);

        Tablero tablero = Tablero.crearTableroTiendaConCeros(idDueño, periodo);

        Tablero guardado = adapter.save(tablero);
        assertNotNull(guardado);
        assertEquals(idDueño, guardado.getIdDueño());
        assertEquals(TipoTablero.TIENDA, guardado.getTipo());

        Optional<Tablero> recuperado = adapter.findByTipoAndIdDueño(TipoTablero.TIENDA, idDueño);
        assertTrue(recuperado.isPresent());
        assertEquals(idDueño, recuperado.get().getIdDueño());
        assertEquals(3, recuperado.get().getIndicadores().size());
    }

    @Test
    @DisplayName("Integración · persistencia: Debe consultar indicadores por tablero y rango de fechas")
    void testConsultarIndicadoresPorPeriodo() {
        UUID idDueño = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 10, 1);
        LocalDate fin = LocalDate.of(2026, 10, 31);

        TableroEntity tableroEntity = TableroEntity.builder()
                .id(UUID.randomUUID())
                .idDueño(idDueño)
                .tipo(TipoTablero.TIENDA)
                .build();
        tableroJpaRepository.save(tableroEntity);

        IndicadorEntity indicadorEntity = IndicadorEntity.builder()
                .id(UUID.randomUUID())
                .tablero(tableroEntity)
                .nombre("Visitas a la página")
                .valor(BigDecimal.valueOf(250))
                .unidad("VISITAS")
                .fecha(LocalDateTime.now())
                .periodoInicio(inicio)
                .periodoFin(fin)
                .valorAnterior(BigDecimal.valueOf(200))
                .variacionPorcentual(BigDecimal.valueOf(25.00))
                .build();
        indicadorJpaRepository.save(indicadorEntity);

        List<Indicador> encontrados = adapter.findIndicadoresByTableroIdAndPeriodo(tableroEntity.getId(), inicio, fin);

        assertNotNull(encontrados);
        assertEquals(1, encontrados.size());
        assertEquals("Visitas a la página", encontrados.get(0).getNombre());
        assertEquals(0, BigDecimal.valueOf(250).compareTo(encontrados.get(0).getValor()));
        assertEquals(UnidadMedida.VISITAS, encontrados.get(0).getUnidad());
    }

    @Test
    @DisplayName("Integración · persistencia: Debe guardar y recuperar tablero con graficas, series y puntos")
    void testGuardarYRecuperarTableroConGraficasYSeriesViaAdapter() {
        UUID idDueño = UUID.randomUUID();
        Punto punto = Punto.builder()
                .id(UUID.randomUUID())
                .fecha(LocalDate.now())
                .valor(BigDecimal.valueOf(120))
                .build();

        Serie serie = Serie.builder()
                .id(UUID.randomUUID())
                .nombre("Serie Diaria")
                .tipo(edu.dosw.styleradar.estadisticas.domain.model.TipoSerie.DIARIA)
                .puntos(new java.util.ArrayList<>(List.of(punto)))
                .build();

        Grafica grafica = Grafica.builder()
                .id(UUID.randomUUID())
                .titulo("Histórico Visitas")
                .series(new java.util.ArrayList<>(List.of(serie)))
                .build();

        Tablero tablero = Tablero.builder()
                .id(UUID.randomUUID())
                .idDueño(idDueño)
                .tipo(TipoTablero.ADMIN)
                .indicadores(new java.util.ArrayList<>())
                .graficas(new java.util.ArrayList<>(List.of(grafica)))
                .build();

        Tablero guardado = adapter.save(tablero);
        assertNotNull(guardado);
        assertEquals(1, guardado.getGraficas().size());

        Optional<Tablero> recuperado = adapter.findByTipoAndIdDueño(TipoTablero.ADMIN, idDueño);
        assertTrue(recuperado.isPresent());
        assertEquals(1, recuperado.get().getGraficas().size());
        assertEquals("Histórico Visitas", recuperado.get().getGraficas().get(0).getTitulo());
        assertEquals(1, recuperado.get().getGraficas().get(0).getSeries().size());
        assertEquals("Serie Diaria", recuperado.get().getGraficas().get(0).getSeries().get(0).getNombre());
        assertEquals(1, recuperado.get().getGraficas().get(0).getSeries().get(0).getPuntos().size());
    }

    @Test
    @DisplayName("Integración · persistencia: Debe manejar unidad desconocida en IndicadorEntity")
    void testUnidadDesconocidaFallback() {
        IndicadorEntity entity = IndicadorEntity.builder()
                .id(UUID.randomUUID())
                .nombre("Test Desconocido")
                .valor(BigDecimal.ONE)
                .unidad("UNIDAD_DESCONOCIDA")
                .fecha(LocalDateTime.now())
                .build();

        Indicador domain = adapter.toDomainIndicador(entity);
        assertNotNull(domain);
        assertEquals(UnidadMedida.CANTIDAD, domain.getUnidad());
    }

    @Test
    @DisplayName("Integración · persistencia: Mapeos toDomain y toEntity con nulos")
    void testAdapterMapeosNulos() {
        assertNull(adapter.toDomain(null));
        assertNull(adapter.toDomainIndicador(null));
        assertNull(adapter.toEntity(null));
        assertNull(adapter.toEntityIndicador(null, null));
    }
}

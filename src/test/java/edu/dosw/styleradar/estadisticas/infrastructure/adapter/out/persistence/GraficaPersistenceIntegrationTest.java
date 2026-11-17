package edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence;

import edu.dosw.styleradar.estadisticas.domain.model.Grafica;
import edu.dosw.styleradar.estadisticas.domain.model.Periodo;
import edu.dosw.styleradar.estadisticas.domain.model.Punto;
import edu.dosw.styleradar.estadisticas.domain.model.Serie;
import edu.dosw.styleradar.estadisticas.domain.model.Tablero;
import edu.dosw.styleradar.estadisticas.domain.model.TipoSerie;
import edu.dosw.styleradar.estadisticas.domain.model.TipoTablero;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.GraficaEntity;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.PuntoEntity;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.SerieEntity;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.TableroEntity;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.GraficaJpaRepository;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.IndicadorJpaRepository;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.PuntoJpaRepository;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.TableroJpaRepository;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.TableroRepositoryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class GraficaPersistenceIntegrationTest {

    @Autowired
    private TableroJpaRepository tableroJpaRepository;

    @Autowired
    private IndicadorJpaRepository indicadorJpaRepository;

    @Autowired
    private GraficaJpaRepository graficaJpaRepository;

    @Autowired
    private PuntoJpaRepository puntoJpaRepository;

    private TableroRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TableroRepositoryAdapter(
                tableroJpaRepository,
                indicadorJpaRepository,
                graficaJpaRepository,
                puntoJpaRepository
        );
    }

    @Test
    @DisplayName("Integración · persistencia: Debe guardar y recuperar gráfica con series y puntos vía adapter")
    void testGuardarYRecuperarGraficaConSeriesYPuntos() {
        UUID idDueño = UUID.randomUUID();
        TableroEntity tableroEntity = TableroEntity.builder()
                .id(UUID.randomUUID())
                .idDueño(idDueño)
                .tipo(TipoTablero.TIENDA)
                .build();
        tableroJpaRepository.save(tableroEntity);

        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 3);
        Periodo periodo = new Periodo(inicio, fin);

        Grafica grafica = Grafica.crearActividadDiariaTiendaConCeros(UUID.randomUUID(), periodo);
        // Modificar un punto de visitas
        grafica.getSeries().get(0).getPuntos().get(0).setValor(BigDecimal.valueOf(42));

        Grafica guardada = adapter.saveGrafica(grafica, tableroEntity.getId());

        assertNotNull(guardada);
        assertEquals(grafica.getId(), guardada.getId());
        assertEquals(Grafica.TITULO_ACTIVIDAD_DIARIA, guardada.getTitulo());

        // Recuperar por ID
        Optional<Grafica> recuperadaOpt = adapter.findGraficaById(guardada.getId());
        assertTrue(recuperadaOpt.isPresent());
        Grafica recuperada = recuperadaOpt.get();
        assertEquals(3, recuperada.getSeries().size());

        // Recuperar por Tablero ID y Título
        Optional<Grafica> porTableroOpt = adapter.findGraficaByTableroIdAndTitulo(
                tableroEntity.getId(), Grafica.TITULO_ACTIVIDAD_DIARIA
        );
        assertTrue(porTableroOpt.isPresent());
        assertEquals(guardada.getId(), porTableroOpt.get().getId());
    }

    @Test
    @DisplayName("Integración · persistencia: Debe consultar puntos de una serie dentro de un rango de fechas")
    void testConsultarPuntosPorSerieYPeriodo() {
        TableroEntity tablero = tableroJpaRepository.save(TableroEntity.builder()
                .id(UUID.randomUUID())
                .idDueño(UUID.randomUUID())
                .tipo(TipoTablero.TIENDA)
                .build());

        GraficaEntity grafica = graficaJpaRepository.save(GraficaEntity.builder()
                .id(UUID.randomUUID())
                .tablero(tablero)
                .titulo("Actividad")
                .build());

        SerieEntity serie = SerieEntity.builder()
                .id(UUID.randomUUID())
                .grafica(grafica)
                .nombre("Visitas")
                .tipo(TipoSerie.DIARIA)
                .build();

        PuntoEntity p1 = PuntoEntity.builder()
                .id(UUID.randomUUID())
                .serie(serie)
                .fecha(LocalDate.of(2026, 11, 1))
                .valor(BigDecimal.valueOf(10))
                .build();

        PuntoEntity p2 = PuntoEntity.builder()
                .id(UUID.randomUUID())
                .serie(serie)
                .fecha(LocalDate.of(2026, 11, 5))
                .valor(BigDecimal.valueOf(20))
                .build();

        PuntoEntity pFueraDeRango = PuntoEntity.builder()
                .id(UUID.randomUUID())
                .serie(serie)
                .fecha(LocalDate.of(2026, 11, 15))
                .valor(BigDecimal.valueOf(99))
                .build();

        serie.setPuntos(new ArrayList<>(List.of(p1, p2, pFueraDeRango)));
        grafica.setSeries(new ArrayList<>(List.of(serie)));
        graficaJpaRepository.save(grafica);

        List<Punto> puntos = adapter.findPuntosBySerieIdAndPeriodo(
                serie.getId(),
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 6)
        );

        assertEquals(2, puntos.size());
        assertEquals(LocalDate.of(2026, 11, 1), puntos.get(0).getFecha());
        assertEquals(0, BigDecimal.valueOf(10).compareTo(puntos.get(0).getValor()));
        assertEquals(LocalDate.of(2026, 11, 5), puntos.get(1).getFecha());
        assertEquals(0, BigDecimal.valueOf(20).compareTo(puntos.get(1).getValor()));
    }

    @Test
    @DisplayName("Integración · persistencia: Métodos de adapter manejan parámetros nulos de forma segura")
    void testParametrosNulosEnAdapter() {
        assertTrue(adapter.findGraficaById(null).isEmpty());
        assertTrue(adapter.findGraficaByTableroIdAndTitulo(null, "Titulo").isEmpty());
        assertTrue(adapter.findGraficaByTableroIdAndTitulo(UUID.randomUUID(), null).isEmpty());
        assertTrue(adapter.findPuntosBySerieIdAndPeriodo(null, LocalDate.now(), LocalDate.now()).isEmpty());
        assertTrue(adapter.findPuntosBySerieIdAndPeriodo(UUID.randomUUID(), null, LocalDate.now()).isEmpty());
        assertTrue(adapter.findPuntosBySerieIdAndPeriodo(UUID.randomUUID(), LocalDate.now(), null).isEmpty());

        assertThrows(IllegalArgumentException.class, () -> adapter.saveGrafica(null, UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class, () -> adapter.saveGrafica(new Grafica(), null));
    }
}

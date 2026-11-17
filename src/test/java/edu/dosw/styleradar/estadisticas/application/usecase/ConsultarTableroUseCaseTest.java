package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.model.Comparacion;
import edu.dosw.styleradar.estadisticas.domain.model.Indicador;
import edu.dosw.styleradar.estadisticas.domain.model.Periodo;
import edu.dosw.styleradar.estadisticas.domain.model.Tablero;
import edu.dosw.styleradar.estadisticas.domain.model.TipoTablero;
import edu.dosw.styleradar.estadisticas.domain.model.UnidadMedida;
import edu.dosw.styleradar.estadisticas.domain.port.out.TableroRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConsultarTableroUseCaseTest {

    private FakeTableroRepositoryPort fakeRepository;
    private ConsultarTableroUseCaseImpl useCase;

    private final UUID idDueño = UUID.randomUUID();
    private final UUID tableroId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        fakeRepository = new FakeTableroRepositoryPort();
        useCase = new ConsultarTableroUseCaseImpl(fakeRepository);
    }

    @Test
    @DisplayName("Criterio 2: Dado un almacén sin registros en el periodo, todos se visualizan en valor cero")
    void testAlmacenSinRegistrosEnPeriodoRetornaCeros() {
        LocalDate inicio = LocalDate.of(2026, 10, 1);
        LocalDate fin = LocalDate.of(2026, 10, 31);

        Tablero tableroExistente = Tablero.builder()
                .id(tableroId)
                .idDueño(idDueño)
                .tipo(TipoTablero.TIENDA)
                .indicadores(new ArrayList<>())
                .graficas(new ArrayList<>())
                .build();
        fakeRepository.tablero = tableroExistente;

        Tablero resultado = useCase.consultarTablero(TipoTablero.TIENDA, idDueño, inicio, fin);

        assertNotNull(resultado);
        assertEquals(3, resultado.getIndicadores().size());

        for (Indicador ind : resultado.getIndicadores()) {
            assertEquals(BigDecimal.ZERO, ind.getValor(), "El valor debe ser cero para " + ind.getNombre());
            assertEquals(BigDecimal.ZERO.setScale(2), ind.getComparacion().getValorAnterior());
            assertEquals(BigDecimal.ZERO.setScale(2), ind.getComparacion().getVariacionPorcentual());
            assertNotNull(ind.getPeriodo());
            assertEquals(inicio, ind.getPeriodo().getInicio());
            assertEquals(fin, ind.getPeriodo().getFin());
        }
    }

    @Test
    @DisplayName("Criterio 1: Dado el administrador autenticado, cuando elige el periodo, presenta visitas, mensajes y prendas con variación")
    void testAlmacenConRegistrosPresentaIndicadoresConVariacion() {
        LocalDate inicio = LocalDate.of(2026, 10, 1);
        LocalDate fin = LocalDate.of(2026, 10, 31);
        Periodo periodo = new Periodo(inicio, fin);
        Periodo anterior = periodo.calcularPeriodoAnterior();

        Tablero tableroExistente = Tablero.builder()
                .id(tableroId)
                .idDueño(idDueño)
                .tipo(TipoTablero.TIENDA)
                .indicadores(new ArrayList<>())
                .graficas(new ArrayList<>())
                .build();
        fakeRepository.tablero = tableroExistente;

        // Indicadores actuales
        fakeRepository.indicadores.add(Indicador.builder()
                .id(UUID.randomUUID())
                .nombre(ConsultarTableroUseCaseImpl.INDICADOR_VISITAS)
                .valor(BigDecimal.valueOf(150))
                .unidad(UnidadMedida.VISITAS)
                .fecha(LocalDateTime.now())
                .periodo(periodo)
                .build());
        fakeRepository.indicadores.add(Indicador.builder()
                .id(UUID.randomUUID())
                .nombre(ConsultarTableroUseCaseImpl.INDICADOR_MENSAJES)
                .valor(BigDecimal.valueOf(40))
                .unidad(UnidadMedida.MENSAJES)
                .fecha(LocalDateTime.now())
                .periodo(periodo)
                .build());
        fakeRepository.indicadores.add(Indicador.builder()
                .id(UUID.randomUUID())
                .nombre(ConsultarTableroUseCaseImpl.INDICADOR_WISHLIST)
                .valor(BigDecimal.valueOf(90))
                .unidad(UnidadMedida.PRENDAS)
                .fecha(LocalDateTime.now())
                .periodo(periodo)
                .build());

        // Indicadores anteriores
        fakeRepository.indicadores.add(Indicador.builder()
                .id(UUID.randomUUID())
                .nombre(ConsultarTableroUseCaseImpl.INDICADOR_VISITAS)
                .valor(BigDecimal.valueOf(100))
                .unidad(UnidadMedida.VISITAS)
                .fecha(LocalDateTime.now().minusMonths(1))
                .periodo(anterior)
                .build());
        fakeRepository.indicadores.add(Indicador.builder()
                .id(UUID.randomUUID())
                .nombre(ConsultarTableroUseCaseImpl.INDICADOR_MENSAJES)
                .valor(BigDecimal.valueOf(50))
                .unidad(UnidadMedida.MENSAJES)
                .fecha(LocalDateTime.now().minusMonths(1))
                .periodo(anterior)
                .build());
        fakeRepository.indicadores.add(Indicador.builder()
                .id(UUID.randomUUID())
                .nombre(ConsultarTableroUseCaseImpl.INDICADOR_WISHLIST)
                .valor(BigDecimal.valueOf(60))
                .unidad(UnidadMedida.PRENDAS)
                .fecha(LocalDateTime.now().minusMonths(1))
                .periodo(anterior)
                .build());

        Tablero resultado = useCase.consultarTablero(TipoTablero.TIENDA, idDueño, inicio, fin);

        assertNotNull(resultado);
        assertEquals(3, resultado.getIndicadores().size());

        // Visitas: 100 -> 150 = +50.00%
        Indicador visitas = resultado.getIndicadores().stream()
                .filter(i -> i.getNombre().equals(ConsultarTableroUseCaseImpl.INDICADOR_VISITAS))
                .findFirst().orElseThrow();
        assertEquals(BigDecimal.valueOf(150), visitas.getValor());
        assertEquals(BigDecimal.valueOf(100).setScale(2), visitas.getComparacion().getValorAnterior());
        assertEquals(BigDecimal.valueOf(50.00).setScale(2), visitas.getComparacion().getVariacionPorcentual());

        // Mensajes: 50 -> 40 = -20.00%
        Indicador mensajes = resultado.getIndicadores().stream()
                .filter(i -> i.getNombre().equals(ConsultarTableroUseCaseImpl.INDICADOR_MENSAJES))
                .findFirst().orElseThrow();
        assertEquals(BigDecimal.valueOf(40), mensajes.getValor());
        assertEquals(BigDecimal.valueOf(50).setScale(2), mensajes.getComparacion().getValorAnterior());
        assertEquals(BigDecimal.valueOf(-20.00).setScale(2), mensajes.getComparacion().getVariacionPorcentual());

        // Wishlist: 60 -> 90 = +50.00%
        Indicador wishlist = resultado.getIndicadores().stream()
                .filter(i -> i.getNombre().equals(ConsultarTableroUseCaseImpl.INDICADOR_WISHLIST))
                .findFirst().orElseThrow();
        assertEquals(BigDecimal.valueOf(90), wishlist.getValor());
        assertEquals(BigDecimal.valueOf(60).setScale(2), wishlist.getComparacion().getValorAnterior());
        assertEquals(BigDecimal.valueOf(50.00).setScale(2), wishlist.getComparacion().getVariacionPorcentual());
    }

    @Test
    @DisplayName("Debe crear tablero con ceros si el tablero aún no existía en el repositorio")
    void testCrearNuevoTableroSiNoExiste() {
        LocalDate inicio = LocalDate.of(2026, 10, 1);
        LocalDate fin = LocalDate.of(2026, 10, 31);

        fakeRepository.tablero = null;

        Tablero resultado = useCase.consultarTablero(TipoTablero.TIENDA, idDueño, inicio, fin);

        assertNotNull(resultado);
        assertTrue(fakeRepository.saveCalled);
        assertEquals(3, resultado.getIndicadores().size());
    }

    @Test
    @DisplayName("Debe resolver el periodo por defecto al mes actual si las fechas son nulas")
    void testPeriodoPorDefectoSiFechasNulas() {
        fakeRepository.tablero = null;

        Tablero resultado = useCase.consultarTablero(TipoTablero.TIENDA, idDueño, null, null);

        assertNotNull(resultado);
        assertNotNull(resultado.getIndicadores().get(0).getPeriodo());
        assertEquals(LocalDate.now().withDayOfMonth(1), resultado.getIndicadores().get(0).getPeriodo().getInicio());
        assertEquals(LocalDate.now(), resultado.getIndicadores().get(0).getPeriodo().getFin());
    }

    @Test
    @DisplayName("Debe consultar y guardar tablero no tienda correctamente (ADMIN o FUNDACION)")
    void testTableroAdminOFundacion() {
        fakeRepository.tablero = null;

        Tablero resultado = useCase.consultarTablero(TipoTablero.ADMIN, idDueño, LocalDate.now(), LocalDate.now());

        assertNotNull(resultado);
        assertEquals(TipoTablero.ADMIN, resultado.getTipo());
        assertTrue(fakeRepository.saveCalled);
    }

    static class FakeTableroRepositoryPort implements TableroRepositoryPort {
        Tablero tablero;
        List<Indicador> indicadores = new ArrayList<>();
        boolean saveCalled = false;

        @Override
        public Optional<Tablero> findByTipoAndIdDueño(TipoTablero tipo, UUID idDueño) {
            return Optional.ofNullable(tablero);
        }

        @Override
        public Tablero save(Tablero tablero) {
            this.tablero = tablero;
            this.saveCalled = true;
            return tablero;
        }

        @Override
        public List<Indicador> findIndicadoresByTableroIdAndPeriodo(UUID tableroId, LocalDate fechaInicio, LocalDate fechaFin) {
            return indicadores.stream()
                    .filter(i -> i.getPeriodo() != null
                            && !i.getPeriodo().getInicio().isBefore(fechaInicio)
                            && !i.getPeriodo().getFin().isAfter(fechaFin))
                    .toList();
        }
    }
}

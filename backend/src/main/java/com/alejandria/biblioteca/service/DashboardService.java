package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.LibroDao;
import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dao.ReservaDao;
import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.reporte.DashboardDTO;
import com.alejandria.biblioteca.dto.reporte.DashboardDTO.Conteo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dashboard del administrador: números del catálogo, de hoy y del mes,
 * y los 4 indicadores de gestión del apartado 1.2.2 del informe.
 */
// dashboard: junta en un solo resumen los conteos q se muestran en la pestaña "Dashboard" del admin
@Service
public class DashboardService {

    private static final Locale ESPANOL = Locale.of("es", "PE");
    private static final DateTimeFormatter FORMATO_MES = DateTimeFormatter.ofPattern("MMMM yyyy", ESPANOL);
    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern("EEE dd/MM", ESPANOL);

    private final PrestamoDao prestamoDao;
    private final LibroDao libroDao;
    private final ReservaDao reservaDao;
    private final UsuarioDao usuarioDao;

    public DashboardService(PrestamoDao prestamoDao, LibroDao libroDao, ReservaDao reservaDao, UsuarioDao usuarioDao) {
        this.prestamoDao = prestamoDao;
        this.libroDao = libroDao;
        this.reservaDao = reservaDao;
        this.usuarioDao = usuarioDao;
    }

    @Transactional
    public DashboardDTO obtener() {
        LocalDateTime ahora = LocalDateTime.now();
        prestamoDao.marcarVencidos(ahora); // para que "vencidos" esté al día

        // el mes actual: desde el día 1 a las 00:00 hasta el día 1 del mes siguiente
        LocalDateTime inicioMes = ahora.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMes = inicioMes.plusMonths(1);

        // catálogo: títulos y ejemplares por estado (sin los dados de baja)
        List<Conteo> librosPorEstado = new ArrayList<>();
        long titulos = 0;
        long ejemplares = 0;
        for (Object[] fila : libroDao.contarPorEstado()) {
            long cantidad = numero(fila[1]);
            librosPorEstado.add(new Conteo((String) fila[0], cantidad));
            titulos += cantidad;
            ejemplares += numero(fila[2]);
        }

        long prestamosMes = prestamoDao.contarPrestamosEntre(inicioMes, finMes);
        long conRetraso = prestamoDao.contarDevueltosConRetrasoEntre(inicioMes, finMes);
        long conIncidencia = prestamoDao.contarPrestamosConIncidenciaEntre(inicioMes, finMes);

        return new DashboardDTO(
                ahora,
                FORMATO_MES.format(inicioMes),
                titulos,
                ejemplares,
                librosPorEstado,
                prestamoDao.countByEstadoPrestamo("activo"),
                prestamoDao.countByEstadoPrestamo("vencido"),
                reservaDao.countByEstadoReserva("pendiente"),
                usuarioDao.contarPrestatarios(),
                prestamosMes,
                prestamoDao.contarDevueltosEntre(inicioMes, finMes),
                conRetraso,
                prestamoDao.contarDevueltosDanadosEntre(inicioMes, finMes),
                IndicadoresGestion.calcular(prestamosMes, conRetraso, conIncidencia, "este mes"),
                aConteos(prestamoDao.prestamosPorCategoriaEntre(inicioMes, finMes)),
                ultimos7Dias(ahora.toLocalDate())
        );
    }

    /** Los últimos 7 días (hoy incluido), con 0 en los días sin préstamos. */
    private List<Conteo> ultimos7Dias(LocalDate hoy) {
        LocalDate primerDia = hoy.minusDays(6);
        Map<LocalDate, Long> porDia = new HashMap<>();
        for (Object[] fila : prestamoDao.prestamosPorDiaDesde(primerDia.atStartOfDay())) {
            porDia.put(LocalDate.parse(fila[0].toString()), numero(fila[1]));
        }
        List<Conteo> dias = new ArrayList<>();
        for (LocalDate dia = primerDia; !dia.isAfter(hoy); dia = dia.plusDays(1)) {
            dias.add(new Conteo(FORMATO_DIA.format(dia), porDia.getOrDefault(dia, 0L)));
        }
        return dias;
    }

    private List<Conteo> aConteos(List<Object[]> filas) {
        return filas.stream().map(f -> new Conteo((String) f[0], numero(f[1]))).toList();
    }

    /** Las consultas nativas devuelven Long o BigDecimal según la función (COUNT, SUM). */
    private long numero(Object valor) {
        return ((Number) valor).longValue();
    }
}

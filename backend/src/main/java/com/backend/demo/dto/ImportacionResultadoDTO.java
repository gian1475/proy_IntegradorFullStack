package com.backend.demo.dto;

import java.util.List;

/** Resultado de importar libros desde un CSV: cuántos se crearon y qué filas fallaron. */
public record ImportacionResultadoDTO(int creados, List<String> errores) {}

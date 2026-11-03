package com.alejandria.biblioteca.dto.libro;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Datos que envía el Administrador para crear o actualizar un libro. */
// datos del libro: estos son los campos q llena el admin en el formulario para registrar o editar.
// validaciones del libro: q no falte ningun dato obligatorio, stock >= 0 y año entre 1000 y 2100
public record LibroFormDTO(
        @NotBlank(message = "Complete todos los campos obligatorios del libro") String titulo,
        @NotBlank(message = "Complete todos los campos obligatorios del libro") String isbn,

        @NotNull(message = "Complete todos los campos obligatorios del libro")
        @Min(value = 1000, message = "El año de publicación no es válido")
        @Max(value = 2100, message = "El año de publicación no es válido")
        Integer anioPublicacion,

        @NotBlank(message = "Complete todos los campos obligatorios del libro") String editorial,

        @NotNull(message = "Complete todos los campos obligatorios del libro")
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock,

        @NotNull(message = "Complete todos los campos obligatorios del libro") Integer idCategoria,
        List<Integer> idsAutores
) {}

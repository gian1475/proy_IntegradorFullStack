package com.alejandria.biblioteca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class BibliotecaApplication {

	public static final String ZONA_HORARIA = "America/Lima";

	public static void main(String[] args) {
		fijarZonaHoraria(); // antes de arrancar Spring, para que todo use la misma hora
		SpringApplication.run(BibliotecaApplication.class, args);
	}

	// hora de lima: el backend siempre trabaja en hora de Lima, aunque la PC o el servidor esten en otra zona.
	// sin esto java toma la zona de la PC: en un servidor en GMT el horario de 8:00 a 20:00, los vencimientos
	// y las sanciones se correrian 5 horas. TimeZone es de Java, no es una libreria aparte
	static void fijarZonaHoraria() {
		TimeZone.setDefault(TimeZone.getTimeZone(ZONA_HORARIA));
	}

}

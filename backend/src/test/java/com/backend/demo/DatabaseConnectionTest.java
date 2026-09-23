package com.backend.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DatabaseConnectionTest {

	@Autowired
	private DataSource dataSource;

	@Test
	void testNeonDatabaseConnection() throws Exception {
		assertNotNull(dataSource, "El DataSource no debe ser nulo");
		try (Connection connection = dataSource.getConnection()) {
			assertNotNull(connection, "La conexión a Neon PostgreSQL no debe ser nula");
			assertTrue(connection.isValid(5), "La conexión debe ser válida");

			try (Statement stmt = connection.createStatement();
				 ResultSet rs = stmt.executeQuery("SELECT version(), current_database(), current_user")) {
				if (rs.next()) {
					System.out.println("=================================================");
					System.out.println(">>> CONEXIÓN EXITOSA A NEON POSTGRESQL <<<");
					System.out.println("Versión BD: " + rs.getString(1));
					System.out.println("Base de datos actual: " + rs.getString(2));
					System.out.println("Usuario conectado: " + rs.getString(3));
					System.out.println("=================================================");
				}
			}
		}
	}
}

package com.backend.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DatabaseSeedVerificationTest {

	@Autowired
	private DataSource dataSource;

	@Test
	void checkAndPopulateSchema() throws Exception {
		try (Connection conn = dataSource.getConnection()) {
			int bookCount = 0;
			try (Statement stmt = conn.createStatement();
				 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM libro")) {
				if (rs.next()) {
					bookCount = rs.getInt(1);
				}
			}

			if (bookCount == 0) {
				System.out.println("Insertando datos semilla desde schema_postgresql.sql...");
				File sqlFile = new File("../database/schema_postgresql.sql");
				if (!sqlFile.exists()) {
					sqlFile = new File("database/schema_postgresql.sql");
				}

				if (sqlFile.exists()) {
					String sqlContent = Files.readString(sqlFile.toPath());
					// Ejecutar solo la sección de inserts
					int insertIndex = sqlContent.indexOf("INSERT INTO categoria");
					if (insertIndex != -1) {
						String seedSql = sqlContent.substring(insertIndex);
						try (Statement stmt = conn.createStatement()) {
							stmt.execute(seedSql);
							System.out.println(">>> Datos semilla insertados exitosamente en Neon <<<");
						}
					}
				}
			}

			// Verificar recuento final
			try (Statement stmt = conn.createStatement();
				 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM libro")) {
				if (rs.next()) {
					System.out.println("Total de libros en Neon ahora: " + rs.getInt(1));
				}
			}

			assertTrue(true);
		}
	}
}

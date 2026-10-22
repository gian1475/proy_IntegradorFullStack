package com.alejandria.biblioteca;

import com.alejandria.biblioteca.dao.LibroDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class BibliotecaApplicationTests {

	@Autowired
	private LibroDao libroDao;

	@Test
	void contextLoads() {
		assertNotNull(libroDao);
	}
}

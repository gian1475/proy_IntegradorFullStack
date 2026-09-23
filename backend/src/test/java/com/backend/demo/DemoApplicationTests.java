package com.backend.demo;

import com.backend.demo.dao.LibroDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class DemoApplicationTests {

	@Autowired
	private LibroDao libroDao;

	@Test
	void contextLoads() {
		assertNotNull(libroDao);
	}
}

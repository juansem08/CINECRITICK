package com.cinecritik;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Este es un proyecto de demostración con Spring Boot que complementa el 
 * backend principal Java HttpServer (CineCritik).
 * 
 * Funciona de forma independiente en el puerto 8080 y se conecta a MongoDB.
 * 
 * Para compilar y ejecutar esta demo de Spring Boot:
 * 1. Abre una terminal en la carpeta spring-demo/
 * 2. Ejecuta: mvn spring-boot:run
 * 3. El servidor iniciará en http://localhost:8080
 * 4. El backend principal de CineCritik se ejecuta en http://localhost:7071
 * Ambos pueden ejecutarse simultáneamente.
 * 
 * Prueba de endpoints con curl o en el navegador:
 * GET  http://localhost:8080/spring/catalogo
 * POST http://localhost:8080/spring/catalogo
 * GET  http://localhost:8080/spring/catalogo/tipo/Pelicula
 * GET  http://localhost:8080/spring/catalogo/tipo/Serie
 * GET  http://localhost:8080/spring/catalogo/tipo/Anime
 */
@SpringBootApplication
public class CineCritikApplication {

    public static void main(String[] args) {
        SpringApplication.run(CineCritikApplication.class, args);
    }
}

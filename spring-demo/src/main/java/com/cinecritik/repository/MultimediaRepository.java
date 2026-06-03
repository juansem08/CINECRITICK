package com.cinecritik.repository;

import com.cinecritik.model.MultimediaDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

/**
 * Repositorio de Spring Data MongoDB para operaciones CRUD en la colección "catalogo".
 */
public interface MultimediaRepository extends MongoRepository<MultimediaDocument, String> {

    // Método personalizado para buscar por tipo (Pelicula, Serie, Anime)
    List<MultimediaDocument> findByTipo(String tipo);
    
    // Método personalizado para buscar por año mayor o igual
    List<MultimediaDocument> findByAnioGreaterThanEqual(int anio);

}

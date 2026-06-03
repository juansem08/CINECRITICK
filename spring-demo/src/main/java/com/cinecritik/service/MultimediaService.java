package com.cinecritik.service;

import com.cinecritik.model.MultimediaDocument;
import com.cinecritik.repository.MultimediaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Capa de Servicios (Business Logic Layer) para la gestión del catálogo de CineCritik en Spring Boot.
 * Implementa la separación de responsabilidades y la validación/normalización de datos.
 */
@Service
public class MultimediaService {

    private final MultimediaRepository repository;

    public MultimediaService(MultimediaRepository repository) {
        this.repository = repository;
    }

    /**
     * Obtiene todo el catálogo de películas, series y anime.
     */
    public List<MultimediaDocument> obtenerTodos() {
        return repository.findAll();
    }

    /**
     * Busca un elemento del catálogo por su ID único de MongoDB.
     */
    public Optional<MultimediaDocument> obtenerPorId(String id) {
        return repository.findById(id);
    }

    /**
     * Filtra elementos del catálogo según el tipo (Pelicula, Serie, Anime).
     */
    public List<MultimediaDocument> obtenerPorTipo(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return repository.findAll();
        }
        // Normalizar primera letra a mayúscula para que coincida con el estándar (ej: "pelicula" -> "Pelicula")
        String tipoNormalizado = tipo.substring(0, 1).toUpperCase() + tipo.substring(1).toLowerCase();
        return repository.findByTipo(tipoNormalizado);
    }

    /**
     * Guarda o actualiza un documento multimedia en MongoDB, aplicando validaciones y normalizaciones.
     */
    public MultimediaDocument guardar(MultimediaDocument doc) {
        if (doc.getTitulo() == null || doc.getTitulo().trim().isEmpty()) {
            throw new IllegalArgumentException("El título del contenido multimedia es obligatorio.");
        }

        // Normalización de datos antes de persistir
        doc.setTitulo(doc.getTitulo().trim());
        
        if (doc.getTipo() != null) {
            String tipoNorm = doc.getTipo().substring(0, 1).toUpperCase() + doc.getTipo().substring(1).toLowerCase();
            doc.setTipo(tipoNorm);
        } else {
            doc.setTipo("Pelicula"); // Tipo por defecto
        }

        if (doc.getAnio() < 1888) { // 1888 fue el año de la primera película grabada
            throw new IllegalArgumentException("El año de lanzamiento debe ser posterior a 1888.");
        }

        return repository.save(doc);
    }

    /**
     * Elimina un elemento del catálogo por su ID. Devuelve true si fue eliminado con éxito.
     */
    public boolean eliminarPorId(String id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Lógica de Negocio Adicional: Obtener películas recientes (lanzadas en 2020 o después).
     */
    public List<MultimediaDocument> obtenerRecomendadasRecientes() {
        return repository.findByAnioGreaterThanEqual(2020);
    }

    /**
     * Lógica de Negocio Adicional: Cuenta el número total de ítems registrados para un tipo.
     */
    public long contarPorTipo(String tipo) {
        List<MultimediaDocument> items = obtenerPorTipo(tipo);
        return items.size();
    }
}

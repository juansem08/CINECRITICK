package com.cinecritik.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Modelo de documento MongoDB que refleja la entidad Multimedia del proyecto principal.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "catalogo")
public class MultimediaDocument {

    @Id
    private String id;
    
    // Tipo: "Pelicula", "Serie" o "Anime"
    private String tipo;
    
    private String titulo;
    private int anio;
    private String genero;
    private String descripcion;
    private String streamingUrl;
    
    // Solo usado si tipo = "Pelicula"
    private int duracionMin;
    
    // Solo usado si tipo = "Serie"
    private int temporadas;
    
    // Solo usado si tipo = "Anime"
    private int episodios;
    
    // Atributos base del contrato Calificable
    private double promedio;
    private int totalVotos;
}

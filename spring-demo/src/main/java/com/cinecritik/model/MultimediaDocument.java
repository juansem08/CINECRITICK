package com.cinecritik.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Modelo de documento MongoDB que refleja la entidad Multimedia del proyecto principal.
 */
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

    // Constructores
    public MultimediaDocument() {
    }

    public MultimediaDocument(String id, String tipo, String titulo, int anio, String genero, 
                              String descripcion, String streamingUrl, int duracionMin, 
                              int temporadas, int episodios, double promedio, int totalVotos) {
        this.id = id;
        this.tipo = tipo;
        this.titulo = titulo;
        this.anio = anio;
        this.genero = genero;
        this.descripcion = descripcion;
        this.streamingUrl = streamingUrl;
        this.duracionMin = duracionMin;
        this.temporadas = temporadas;
        this.episodios = episodios;
        this.promedio = promedio;
        this.totalVotos = totalVotos;
    }

    // Getters y Setters Manuales para evitar problemas de compilación con Lombok
    public String getId() { 
        return id; 
    }
    
    public void setId(String id) { 
        this.id = id; 
    }

    public String getTipo() { 
        return tipo; 
    }
    
    public void setTipo(String tipo) { 
        this.tipo = tipo; 
    }

    public String getTitulo() { 
        return titulo; 
    }
    
    public void setTitulo(String titulo) { 
        this.titulo = titulo; 
    }

    public int getAnio() { 
        return anio; 
    }
    
    public void setAnio(int anio) { 
        this.anio = anio; 
    }

    public String getGenero() { 
        return genero; 
    }
    
    public void setGenero(String genero) { 
        this.genero = genero; 
    }

    public String getDescripcion() { 
        return descripcion; 
    }
    
    public void setDescripcion(String descripcion) { 
        this.descripcion = descripcion; 
    }

    public String getStreamingUrl() { 
        return streamingUrl; 
    }
    
    public void setStreamingUrl(String streamingUrl) { 
        this.streamingUrl = streamingUrl; 
    }

    public int getDuracionMin() { 
        return duracionMin; 
    }
    
    public void setDuracionMin(int duracionMin) { 
        this.duracionMin = duracionMin; 
    }

    public int getTemporadas() { 
        return temporadas; 
    }
    
    public void setTemporadas(int temporadas) { 
        this.temporadas = temporadas; 
    }

    public int getEpisodios() { 
        return episodios; 
    }
    
    public void setEpisodios(int episodios) { 
        this.episodios = episodios; 
    }

    public double getPromedio() { 
        return promedio; 
    }
    
    public void setPromedio(double promedio) { 
        this.promedio = promedio; 
    }

    public int getTotalVotos() { 
        return totalVotos; 
    }
    
    public void setTotalVotos(int totalVotos) { 
        this.totalVotos = totalVotos; 
    }
}

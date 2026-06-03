/**
 * Bloques 5 y 6: Herencia (Pelicula hereda de Multimedia).
 * Bloque 7: Polimorfismo (mensajeMetraje distinto).
 */
class PeliculaM extends Multimedia {
  private int duracionMin;

  public PeliculaM(int id, String titulo, int anio, String genero, String descripcion, String posterDataUrl, String streamingUrl, int duracionMin) {
    super(id, titulo, anio, genero, descripcion, posterDataUrl, streamingUrl);
    this.duracionMin = duracionMin;
  }

  public int getDuracionMin() { return duracionMin; }

  @Override
  public void ejecutarAccion() {
    // Bloque 8: acción “concreta” de la abstracción
    System.out.println("Reproduciendo película: " + titulo);
  }

  @Override
  public String mensajeMetraje() {
    // Bloque 7: polimorfismo (Pelicula)
    return "La cinta se corta en el minuto 13. Duración: " + duracionMin + " min.";
  }

  @Override
  protected String getType() {
    return "Pelicula";
  }

  @Override
  public String toJson() {
    // extiende JSON base con técnico
    String base = super.toJson();
    // insertar antes del cierre
    return base.substring(0, base.length() - 1) + ",\"duracionMin\":" + duracionMin + "}";
  }
}

/**
 * Bloques 5 y 6: Herencia (Serie hereda de Multimedia).
 * Bloque 7: Polimorfismo (mensajeMetraje distinto).
 */
class SerieM extends Multimedia {
  private int temporadas;

  public SerieM(int id, String titulo, int anio, String genero, String descripcion, String posterDataUrl, String streamingUrl, int temporadas) {
    super(id, titulo, anio, genero, descripcion, posterDataUrl, streamingUrl);
    this.temporadas = temporadas;
  }

  public int getTemporadas() { return temporadas; }

  @Override
  public void ejecutarAccion() {
    System.out.println("Abriendo serie: " + titulo);
  }

  @Override
  public String mensajeMetraje() {
    // Bloque 7: polimorfismo (Serie)
    return "Episodios incompletos. Temporadas registradas: " + temporadas + ".";
  }

  @Override
  protected String getType() {
    return "Serie";
  }

  @Override
  public String toJson() {
    String base = super.toJson();
    return base.substring(0, base.length() - 1) + ",\"temporadas\":" + temporadas + "}";
  }
}

/**
 * Bloques 5/6/7: Anime hereda de Multimedia y responde distinto (polimorfismo).
 */
class AnimeM extends Multimedia {
  private int episodios;

  public AnimeM(int id, String titulo, int anio, String genero, String descripcion, String posterDataUrl, String streamingUrl, int episodios) {
    super(id, titulo, anio, genero, descripcion, posterDataUrl, streamingUrl);
    this.episodios = episodios;
  }

  public int getEpisodios() { return episodios; }

  @Override
  public void ejecutarAccion() {
    System.out.println("Ejecutando anime: " + titulo);
  }

  @Override
  public String mensajeMetraje() {
    return "Subtítulos corruptos. Episodios detectados: " + episodios + ".";
  }

  @Override
  protected String getType() {
    return "Anime";
  }

  @Override
  public String toJson() {
    String base = super.toJson();
    return base.substring(0, base.length() - 1) + ",\"episodios\":" + episodios + "}";
  }
}

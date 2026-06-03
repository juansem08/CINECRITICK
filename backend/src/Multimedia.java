/**
 * Bloque 8: Abstracción.
 * Bloque 11: Estructura de datos (atributos base para catálogo).
 */
public abstract class Multimedia implements Calificable {
  // Bloque 11: atributos base (estructura de datos)
  protected int id;
  protected String titulo;
  protected int anio;
  protected String genero;
  protected String descripcion;
  // UI: imagen opcional (data URL). No es parte de la teoría, solo soporte visual.
  protected String posterDataUrl;
  // UI: enlace de streaming externo
  protected String streamingUrl;

  protected int sumaPuntajes = 0;
  protected int totalVotos = 0;

  protected Multimedia(int id, String titulo, int anio, String genero, String descripcion, String posterDataUrl, String streamingUrl) {
    this.id = id;
    this.titulo = titulo;
    this.anio = anio;
    this.genero = genero;
    this.descripcion = descripcion;
    this.posterDataUrl = posterDataUrl;
    this.streamingUrl = streamingUrl;
  }

  public int getId() { return id; }
  public String getTitulo() { return titulo; }
  public int getAnio() { return anio; }
  public String getGenero() { return genero; }
  public String getDescripcion() { return descripcion; }
  public String getPosterDataUrl() { return posterDataUrl; }
  public String getStreamingUrl() { return streamingUrl; }

  @Override
  public void calificar(int puntaje) {
      if (puntaje < 1 || puntaje > 5) {
          throw new CineCritikException("El puntaje debe estar entre 1 y 5");
      }
      this.sumaPuntajes += puntaje;
      this.totalVotos++;
  }

  @Override
  public double getPromedio() {
      if (totalVotos == 0) return 0.0;
      return (double) sumaPuntajes / totalVotos;
  }

  @Override
  public int getTotalVotos() {
      return totalVotos;
  }

  // Bloque 8: método abstracto (no instanciable)
  public abstract void ejecutarAccion();

  // Para UI: respuesta polimórfica “metraje encontrado”
  public abstract String mensajeMetraje();

  public String toJson() {
    return "{"
        + "\"id\":" + id + ","
        + "\"tipo\":\"" + esc(getType()) + "\","
        + "\"titulo\":\"" + esc(titulo) + "\","
        + "\"anio\":" + anio + ","
        + "\"genero\":\"" + esc(genero) + "\","
        + "\"descripcion\":\"" + esc(descripcion) + "\","
        + "\"metraje\":\"" + esc(mensajeMetraje()) + "\","
        + "\"posterDataUrl\":\"" + esc(posterDataUrl) + "\","
        + "\"streamingUrl\":\"" + esc(streamingUrl) + "\","
        + "\"promedio\":" + getPromedio() + ","
        + "\"totalVotos\":" + getTotalVotos()
        + "}";
  }

  protected abstract String getType();

  protected static String esc(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}

import java.time.LocalDateTime;

/**
 * Entrada del foro global (copia de un ítem de catálogo).
 */
class ForumPost {
  private final int id;
  private final int multimediaId;
  private final String usuario;
  private final String titulo;
  private final String tipo;
  private final int anio;
  private final String genero;
  private final String descripcion;
  private final String posterDataUrl;
  private final String streamingUrl;
  private final LocalDateTime creadoEn;

  public ForumPost(int id,
                   int multimediaId,
                   String usuario,
                   String titulo,
                   String tipo,
                   int anio,
                   String genero,
                   String descripcion,
                   String posterDataUrl,
                   String streamingUrl) {
    this.id = id;
    this.multimediaId = multimediaId;
    this.usuario = usuario;
    this.titulo = titulo;
    this.tipo = tipo;
    this.anio = anio;
    this.genero = genero;
    this.descripcion = descripcion;
    this.posterDataUrl = posterDataUrl;
    this.streamingUrl = streamingUrl;
    this.creadoEn = LocalDateTime.now();
  }

  public int getId() { return id; }
  public int getMultimediaId() { return multimediaId; }
  public String getUsuario() { return usuario; }
  public String getTitulo() { return titulo; }
  public String getTipo() { return tipo; }
  public int getAnio() { return anio; }
  public String getGenero() { return genero; }
  public String getDescripcion() { return descripcion; }
  public String getPosterDataUrl() { return posterDataUrl; }
  public String getStreamingUrl() { return streamingUrl; }

  public String toJson() {
    return "{"
        + "\"id\":" + id + ","
        + "\"multimediaId\":" + multimediaId + ","
        + "\"usuario\":\"" + esc(usuario) + "\","
        + "\"titulo\":\"" + esc(titulo) + "\","
        + "\"tipo\":\"" + esc(tipo) + "\","
        + "\"anio\":" + anio + ","
        + "\"genero\":\"" + esc(genero) + "\","
        + "\"descripcion\":\"" + esc(descripcion) + "\","
        + "\"posterDataUrl\":\"" + esc(posterDataUrl) + "\","
        + "\"streamingUrl\":\"" + esc(streamingUrl) + "\","
        + "\"creadoEn\":\"" + esc(creadoEn.toString()) + "\""
        + "}";
  }

  private static String esc(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}

class ForumComment {
  private final int postId;
  private final String usuario;
  private final String texto;
  private final LocalDateTime creadoEn;

  public ForumComment(int postId, String usuario, String texto) {
    this.postId = postId;
    this.usuario = usuario;
    this.texto = texto;
    this.creadoEn = LocalDateTime.now();
  }

  public int getPostId() { return postId; }
  public String getUsuario() { return usuario; }
  public String getTexto() { return texto; }

  public String toJson() {
    return "{"
        + "\"postId\":" + postId + ","
        + "\"usuario\":\"" + esc(usuario) + "\","
        + "\"texto\":\"" + esc(texto) + "\","
        + "\"creadoEn\":\"" + esc(creadoEn.toString()) + "\""
        + "}";
  }

  private static String esc(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}

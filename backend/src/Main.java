import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class Main {
  private static final int PORT = 7071;

  static class SessionInfo {
      String username;
      LocalDateTime timestamp;
      SessionInfo(String username, LocalDateTime timestamp) {
          this.username = username;
          this.timestamp = timestamp;
      }
  }

  public static void main(String[] args) throws Exception {
    DataStore.initDB();

    Properties config = new Properties();
    try (InputStream in = new FileInputStream("config.properties")) {
        config.load(in);
    } catch (IOException e) {
        System.err.println("Advertencia: No se pudo cargar config.properties");
    }

    String adminUser = config.getProperty("admin.username", "andres");
    String adminPass = config.getProperty("admin.password", "cine");

    SOLID_CineCritik.GestorAcceso acceso = new SOLID_CineCritik.GestorAcceso(adminUser, adminPass);
    RepositorioUsuarios repoUsuarios = new RepositorioUsuarios();

    final Map<String, SessionInfo> sessions = new HashMap<>();

    java.util.function.Function<String, CatalogoPeliculas> getCatalog = (String user) -> {
      CatalogoPeliculas c = DataStore.loadCatalog(user);
      if (c.listar().isEmpty()) {
        CatalogoPeliculas.Creador creador = new CatalogoPeliculas.Creador(c);
        creador.crearPelicula("Inception", 2010, "Ciencia Ficción",
            "Una cinta sellada repite tres nombres. Cada acceso deja marcas.", "", "https://www.warnerbros.com/movies/inception", 92);
        creador.crearSerie("Archivos Clasificados", 2026, "Suspenso / Terror",
            "El catálogo crece. Los pósters observan. Los expedientes responden.", "", "https://www.netflix.com", 1);
        DataStore.saveCatalog(user, c);
      }
      return c;
    };

    Espectador[] sujetos = new Espectador[] {
        new FanaticoCine("Andrés"),
        new UsuarioCasual("Sebastian"),
        new CriticoExperto("Aleja")
    };

    HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);

    Path webRootTmp = Paths.get("..", "frontend").toAbsolutePath().normalize();
    if (!Files.exists(webRootTmp)) {
      webRootTmp = Paths.get("frontend").toAbsolutePath().normalize();
    }
    final Path webRoot = webRootTmp;

    server.createContext("/", ex -> {
      serveFile(ex, webRoot.resolve("index.html"), "text/html; charset=utf-8");
    });
    server.createContext("/styles.css", ex ->
        serveFile(ex, webRoot.resolve("styles.css"), "text/css; charset=utf-8"));
    server.createContext("/app.js", ex ->
        serveFile(ex, webRoot.resolve("app.js"), "application/javascript; charset=utf-8"));
    server.createContext("/image_12.jpeg", ex ->
        serveFile(ex, webRoot.resolve("image_12.jpeg"), "image/jpeg"));

    java.util.function.Function<HttpExchange, String> requireUser = (HttpExchange ex) -> {
      Headers h = ex.getRequestHeaders();
      String token = h.getFirst("X-Auth");
      if (token == null || token.trim().isEmpty()) return null;
      SessionInfo info = sessions.get(token);
      if (info == null) return null;
      if (ChronoUnit.HOURS.between(info.timestamp, LocalDateTime.now()) >= 8) {
          sessions.remove(token);
          return null;
      }
      return info.username;
    };

    server.createContext("/api/login", ex -> {
      if (!checkMethod(ex, "POST")) return;
      String body = readUtf8(ex.getRequestBody());
      String username = JsonLite.getString(body, "username").trim().toLowerCase();
      String password = JsonLite.getString(body, "password");

      boolean ok = acceso.validar(username, password) || repoUsuarios.validar(username, password);
      if (!ok) {
        json(ex, 401, "{\"ok\":false,\"error\":\"Credenciales inválidas\"}");
        return;
      }

      String token = UUID.randomUUID().toString();
      sessions.put(token, new SessionInfo(username, LocalDateTime.now()));
      json(ex, 200, "{\"ok\":true,\"token\":\"" + escapeJson(token) + "\",\"username\":\"" + escapeJson(username) + "\",\"ts\":\"" + LocalDateTime.now() + "\"}");
    });

    server.createContext("/api/register", ex -> {
      if (!checkMethod(ex, "POST")) return;
      String body = readUtf8(ex.getRequestBody());
      String username = JsonLite.getString(body, "username").trim().toLowerCase();
      String password = JsonLite.getString(body, "password");

      if (username.length() < 3) {
        json(ex, 400, "{\"ok\":false,\"error\":\"El usuario debe tener al menos 3 caracteres\"}");
        return;
      }
      if (password.length() < 4) {
        json(ex, 400, "{\"ok\":false,\"error\":\"La contraseña debe tener al menos 4 caracteres\"}");
        return;
      }
      if (adminUser.equalsIgnoreCase(username)) {
        json(ex, 400, "{\"ok\":false,\"error\":\"Usuario reservado\"}");
        return;
      }
      boolean exists = repoUsuarios.existe(username);
      if (exists) {
        json(ex, 409, "{\"ok\":false,\"error\":\"El usuario ya existe\"}");
        return;
      }
      boolean created = repoUsuarios.registrar(username, password);
      if (!created) {
        json(ex, 500, "{\"ok\":false,\"error\":\"No se pudo registrar\"}");
        return;
      }
      json(ex, 200, "{\"ok\":true}");
    });

    server.createContext("/api/catalogo", ex -> {
      if (!checkMethod(ex, "GET")) return;
      String user = checkAuth(ex, requireUser);
      if (user == null) return;
      String itemsJson = catalogoJson(getCatalog.apply(user));
      json(ex, 200, "{\"ok\":true,\"items\":" + itemsJson + "}");
    });

    server.createContext("/api/catalogo/agregar", ex -> {
      if (!checkMethod(ex, "POST")) return;
      String user = checkAuth(ex, requireUser);
      if (user == null) return;
      String body = readUtf8(ex.getRequestBody());
      String tipo = JsonLite.getString(body, "tipo");
      String titulo = JsonLite.getString(body, "titulo");
      String genero = JsonLite.getString(body, "genero");
      String descripcion = JsonLite.getString(body, "descripcion");
      String posterDataUrl = JsonLite.getString(body, "posterDataUrl");
      String streamingUrl = JsonLite.getString(body, "streamingUrl");
      int anio = JsonLite.getInt(body, "anio", 2026);
      int duracion = JsonLite.getInt(body, "duracionMin", 90);
      int temporadas = JsonLite.getInt(body, "temporadas", 1);
      int episodios = JsonLite.getInt(body, "episodios", 12);

      if (titulo.trim().isEmpty()) {
        json(ex, 400, "{\"ok\":false,\"error\":\"Título requerido\"}");
        return;
      }

      CatalogoPeliculas c = getCatalog.apply(user);
      CatalogoPeliculas.Creador creador = new CatalogoPeliculas.Creador(c);
      Multimedia created;
      if ("Serie".equalsIgnoreCase(tipo)) {
        created = creador.crearSerie(titulo, anio, genero, descripcion, posterDataUrl, streamingUrl, temporadas);
      } else if ("Anime".equalsIgnoreCase(tipo)) {
        created = creador.crearAnime(titulo, anio, genero, descripcion, posterDataUrl, streamingUrl, episodios);
      } else {
        created = creador.crearPelicula(titulo, anio, genero, descripcion, posterDataUrl, streamingUrl, duracion);
      }
      DataStore.saveCatalog(user, c);
      json(ex, 200, "{\"ok\":true,\"item\":" + created.toJson() + "}");
    });

    server.createContext("/api/catalogo/editar", ex -> {
      if (!checkMethod(ex, "POST")) return;
      String user = checkAuth(ex, requireUser);
      if (user == null) return;
      String body = readUtf8(ex.getRequestBody());
      int id = JsonLite.getInt(body, "id", -1);
      if (id <= 0) {
        json(ex, 400, "{\"ok\":false,\"error\":\"ID inválido\"}");
        return;
      }
      String tipo = JsonLite.getString(body, "tipo");
      String titulo = JsonLite.getString(body, "titulo");
      String genero = JsonLite.getString(body, "genero");
      String descripcion = JsonLite.getString(body, "descripcion");
      String posterDataUrl = JsonLite.getString(body, "posterDataUrl");
      String streamingUrl = JsonLite.getString(body, "streamingUrl");
      int anio = JsonLite.getInt(body, "anio", 2026);
      int duracion = JsonLite.getInt(body, "duracionMin", 90);
      int temporadas = JsonLite.getInt(body, "temporadas", 1);
      int episodios = JsonLite.getInt(body, "episodios", 12);

      CatalogoPeliculas c = getCatalog.apply(user);
      CatalogoPeliculas.Creador creador = new CatalogoPeliculas.Creador(c);
      Multimedia updated = creador.crearConId(id, tipo, titulo, anio, genero, descripcion, posterDataUrl, streamingUrl, duracion, temporadas, episodios);
      if (!c.actualizar(updated)) updated = null;
      if (updated == null) {
        json(ex, 404, "{\"ok\":false,\"error\":\"No existe ese ID\"}");
        return;
      }
      DataStore.saveCatalog(user, c);
      DataStore.updateForumPostSnapshot(id, user, updated);
      json(ex, 200, "{\"ok\":true,\"item\":" + updated.toJson() + "}");
    });

    server.createContext("/api/catalogo/eliminar", ex -> {
      if (!checkMethod(ex, "POST")) return;
      String user = checkAuth(ex, requireUser);
      if (user == null) return;
      String body = readUtf8(ex.getRequestBody());
      int id = JsonLite.getInt(body, "id", -1);
      if (id <= 0) {
        json(ex, 400, "{\"ok\":false,\"error\":\"ID inválido\"}");
        return;
      }
      CatalogoPeliculas c = getCatalog.apply(user);
      boolean ok = c.eliminarPorId(id);
      if (!ok) {
        json(ex, 404, "{\"ok\":false,\"error\":\"No existe ese ID\"}");
        return;
      }
      DataStore.saveCatalog(user, c);
      json(ex, 200, "{\"ok\":true}");
    });
    
    server.createContext("/api/catalogo/calificar", ex -> {
        if (!checkMethod(ex, "POST")) return;
        String user = checkAuth(ex, requireUser);
        if (user == null) return;
        String body = readUtf8(ex.getRequestBody());
        int id = JsonLite.getInt(body, "id", -1);
        int puntaje = JsonLite.getInt(body, "puntaje", -1);
        
        CatalogoPeliculas c = getCatalog.apply(user);
        Multimedia m = c.buscarPorId(id);
        if (m == null) {
            json(ex, 404, "{\"ok\":false,\"error\":\"No existe ese ID\"}");
            return;
        }
        try {
            m.calificar(puntaje);
            json(ex, 200, "{\"ok\":true, \"promedio\":" + m.getPromedio() + ", \"totalVotos\":" + m.getTotalVotos() + "}");
        } catch (CineCritikException e) {
            json(ex, 400, "{\"ok\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    });

    server.createContext("/api/catalogo/exportar", ex -> {
        if (!checkMethod(ex, "GET")) return;
        String user = checkAuth(ex, requireUser);
        if (user == null) return;
        CatalogoPeliculas c = getCatalog.apply(user);
        try {
            Path file = Paths.get("data", "catalogo_" + user + ".csv");
            DataStore.exportarCSV(c.listar(), file.toString());
            json(ex, 200, "{\"ok\":true,\"message\":\"Exportado a " + escapeJson(file.toString()) + "\"}");
        } catch (CineCritikException e) {
            json(ex, 500, "{\"ok\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    });

    server.createContext("/api/info", ex -> {
      if (!checkMethod(ex, "GET")) return;
      String user = checkAuth(ex, requireUser);
      if (user == null) return;
      String catJson = catalogoJson(getCatalog.apply(user));

      ArticuloDeCine articuloDeCine = new ArticuloDeCine();
      String evid = articuloDeCine.analizarPelicula();
      int riesgo = articuloDeCine.calificarRecomendacion();
      String payload = "{"
          + "\"ok\":true,"
          + "\"catalogo\":" + catJson + ","
          + "\"evidencia\":\"" + escapeJson(evid) + "\","
          + "\"riesgo\":" + riesgo + ","
          + "\"sujetos\":" + sujetosJson(sujetos)
          + "}";
      json(ex, 200, payload);
    });

    server.createContext("/api/forum/publicar", ex -> {
      if (!checkMethod(ex, "POST")) return;
      String user = checkAuth(ex, requireUser);
      if (user == null) return;
      String body = readUtf8(ex.getRequestBody());
      int id = JsonLite.getInt(body, "id", -1);
      if (id <= 0) {
        json(ex, 400, "{\"ok\":false,\"error\":\"ID inválido\"}");
        return;
      }
      CatalogoPeliculas c = getCatalog.apply(user);
      Multimedia m = c.buscarPorId(id);
      if (m == null) {
        json(ex, 404, "{\"ok\":false,\"error\":\"No existe ese ID en tu catálogo\"}");
        return;
      }
      List<ForumPost> currentPosts = DataStore.loadForumPosts();
      int nextId = 1;
      for (ForumPost p : currentPosts) {
        if (p.getId() >= nextId) nextId = p.getId() + 1;
      }
      currentPosts.add(new ForumPost(
          nextId,
          m.getId(),
          user,
          m.getTitulo(),
          m.getClass().getSimpleName(),
          m.getAnio(),
          m.getGenero(),
          m.getDescripcion(),
          m.getPosterDataUrl(),
          m.getStreamingUrl()
      ));
      DataStore.saveForumPosts(currentPosts);
      json(ex, 200, "{\"ok\":true}");
    });

    server.createContext("/api/forum/list", ex -> {
      if (!checkMethod(ex, "GET")) return;
      List<ForumPost> posts = DataStore.loadForumPosts();
      Map<Integer, int[]> votes = DataStore.loadForumVotes();

      StringBuilder sb = new StringBuilder();
      sb.append("[");
      for (int i = 0; i < posts.size(); i++) {
        int pid = posts.get(i).getId();
        int likes = votes.containsKey(pid) ? votes.get(pid)[0] : 0;
        int dislikes = votes.containsKey(pid) ? votes.get(pid)[1] : 0;
        String jsonPost = posts.get(i).toJson();
        jsonPost = jsonPost.substring(0, jsonPost.length() - 1) + ",\"likes\":" + likes + ",\"dislikes\":" + dislikes + "}";
        sb.append(jsonPost);
        if (i < posts.size() - 1) sb.append(",");
      }
      sb.append("]");
      json(ex, 200, "{\"ok\":true,\"posts\":" + sb + "}");
    });

    server.createContext("/api/forum/vote", ex -> {
      if (!checkMethod(ex, "POST")) return;
      String user = checkAuth(ex, requireUser);
      if (user == null) return;
      String body = readUtf8(ex.getRequestBody());
      int postId = JsonLite.getInt(body, "postId", -1);
      int isLike = JsonLite.getInt(body, "isLike", 0);
      if (postId <= 0 || (isLike != -1 && isLike != 0 && isLike != 1)) {
        json(ex, 400, "{\"ok\":false,\"error\":\"Parámetros inválidos\"}");
        return;
      }
      DataStore.saveForumVote(postId, user, isLike);
      
      Map<Integer, int[]> allVotes = DataStore.loadForumVotes();
      int[] counts = allVotes.getOrDefault(postId, new int[]{0,0});
      
      json(ex, 200, "{\"ok\":true, \"likes\":" + counts[0] + ", \"dislikes\":" + counts[1] + "}");
    });

    server.createContext("/api/forum/comment", ex -> {
      if (!checkMethod(ex, "POST")) return;
      String user = checkAuth(ex, requireUser);
      if (user == null) return;
      String body = readUtf8(ex.getRequestBody());
      int postId = JsonLite.getInt(body, "postId", -1);
      String texto = JsonLite.getString(body, "texto");
      if (postId <= 0) {
        json(ex, 400, "{\"ok\":false,\"error\":\"postId inválido\"}");
        return;
      }
      if (texto == null || texto.trim().isEmpty()) {
        json(ex, 400, "{\"ok\":false,\"error\":\"Texto requerido\"}");
        return;
      }
      if (texto.length() > 240) {
        json(ex, 400, "{\"ok\":false,\"error\":\"Máximo 240 caracteres\"}");
        return;
      }
      Map<Integer, List<ForumComment>> currentComments = DataStore.loadForumComments();
      List<ForumComment> list = currentComments.get(postId);
      if (list == null) {
        list = new ArrayList<ForumComment>();
        currentComments.put(postId, list);
      }
      list.add(new ForumComment(postId, user, texto.trim()));
      DataStore.saveForumComments(currentComments);
      json(ex, 200, "{\"ok\":true}");
    });

    server.createContext("/api/forum/comments", ex -> {
      if (!checkMethod(ex, "GET")) return;
      String q = ex.getRequestURI().getQuery();
      int postId = -1;
      try {
        if (q != null && q.startsWith("postId=")) {
          postId = Integer.parseInt(q.substring("postId=".length()));
        }
      } catch (NumberFormatException ignored) {}
      if (postId <= 0) {
        json(ex, 400, "{\"ok\":false,\"error\":\"postId requerido\"}");
        return;
      }
      Map<Integer, List<ForumComment>> currentComments = DataStore.loadForumComments();
      List<ForumComment> list = currentComments.get(postId);
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      if (list != null) {
        for (int i = 0; i < list.size(); i++) {
          sb.append(list.get(i).toJson());
          if (i < list.size() - 1) sb.append(",");
        }
      }
      sb.append("]");
      json(ex, 200, "{\"ok\":true,\"comments\":" + sb + "}");
    });

    server.createContext("/api/forum/delete", ex -> {
      if (!checkMethod(ex, "POST")) return;
      String user = checkAuth(ex, requireUser);
      if (user == null) return;
      String body = readUtf8(ex.getRequestBody());
      int postId = JsonLite.getInt(body, "postId", -1);
      
      List<ForumPost> currentPosts = DataStore.loadForumPosts();
      boolean found = false, owner = false;
      for (ForumPost p : currentPosts) {
          if (p.getId() == postId) {
              found = true;
              if (p.getUsuario().equalsIgnoreCase(user)) owner = true;
              break;
          }
      }
      
      if (!found) {
          json(ex, 404, "{\"ok\":false,\"error\":\"No existe la publicación\"}");
          return;
      }
      if (!owner) {
          json(ex, 403, "{\"ok\":false,\"error\":\"No tienes permiso para eliminar este post\"}");
          return;
      }
      
      DataStore.deleteForumPost(postId);
      json(ex, 200, "{\"ok\":true}");
    });



    server.setExecutor(null);
    server.start();
    System.out.println("CineCritik listo en: http://127.0.0.1:" + PORT);
  }

  private static boolean checkMethod(HttpExchange ex, String... methods) throws IOException {
      String reqMethod = ex.getRequestMethod();
      for (String m : methods) {
          if (m.equalsIgnoreCase(reqMethod)) return true;
      }
      json(ex, 405, "{\"ok\":false,\"error\":\"Method not allowed\"}");
      return false;
  }

  private static String checkAuth(HttpExchange ex, java.util.function.Function<HttpExchange, String> requireUser) throws IOException {
      String user = requireUser.apply(ex);
      if (user == null) {
          json(ex, 401, "{\"ok\":false,\"error\":\"No autorizado\"}");
      }
      return user;
  }

  private static void serveFile(HttpExchange ex, Path file, String contentType) throws IOException {
    if (!Files.exists(file) || Files.isDirectory(file)) {
      ex.sendResponseHeaders(404, 0);
      try (OutputStream os = ex.getResponseBody()) {
        os.write("404".getBytes(StandardCharsets.UTF_8));
      }
      return;
    }
    byte[] data = Files.readAllBytes(file);
    Headers h = ex.getResponseHeaders();
    h.set("Content-Type", contentType);
    h.set("Cache-Control", "no-store");
    ex.sendResponseHeaders(200, data.length);
    try (OutputStream os = ex.getResponseBody()) {
      os.write(data);
    }
  }

  private static void json(HttpExchange ex, int status, String payload) throws IOException {
    Headers h = ex.getResponseHeaders();
    h.set("Content-Type", "application/json; charset=utf-8");
    h.set("Cache-Control", "no-store");
    byte[] data = payload.getBytes(StandardCharsets.UTF_8);
    ex.sendResponseHeaders(status, data.length);
    try (OutputStream os = ex.getResponseBody()) {
      os.write(data);
    }
  }

  private static String readUtf8(InputStream is) throws IOException {
    byte[] buffer = new byte[4096];
    int read;
    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    while ((read = is.read(buffer)) != -1) {
      baos.write(buffer, 0, read);
    }
    return new String(baos.toByteArray(), StandardCharsets.UTF_8);
  }

  private static String escapeJson(String s) {
    if (s == null) return "";
    return s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  private static String catalogoJson(CatalogoPeliculas catalogo) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    java.util.List<Multimedia> items = catalogo.listar();
    for (int i = 0; i < items.size(); i++) {
      sb.append(items.get(i).toJson());
      if (i < items.size() - 1) sb.append(",");
    }
    sb.append("]");
    return sb.toString();
  }

  private static String sujetosJson(Espectador[] sujetos) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int i = 0; i < sujetos.length; i++) {
      Espectador s = sujetos[i];
      sb.append("{\"nombre\":\"")
          .append(escapeJson(s.getNombre()))
          .append("\",\"estadoMarcado\":\"")
          .append(escapeJson(s.getNivelCinefilo()))
          .append("\",\"reaccion\":\"")
          .append(escapeJson(s.darOpinion()))
          .append("\"}");
      if (i < sujetos.length - 1) sb.append(",");
    }
    sb.append("]");
    return sb.toString();
  }
}

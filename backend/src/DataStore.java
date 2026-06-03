import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataStore {
    private static final String URL = "jdbc:sqlite:data/cinecritik.db";

    public static void initDB() {
        new File("data").mkdirs();
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS usuarios (" +
                    "username VARCHAR(50) PRIMARY KEY," +
                    "password VARCHAR(255) NOT NULL" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS multimedia (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username VARCHAR(50) NOT NULL," +
                    "tipo VARCHAR(20) NOT NULL," +
                    "titulo VARCHAR(150) NOT NULL," +
                    "anio INTEGER," +
                    "genero VARCHAR(50)," +
                    "descripcion TEXT," +
                    "poster_data_url TEXT," +
                    "streaming_url TEXT," +
                    "duracion_min INTEGER," +
                    "temporadas INTEGER," +
                    "episodios INTEGER," +
                    "FOREIGN KEY (username) REFERENCES usuarios(username)" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS foro_posts (" +
                    "id INTEGER PRIMARY KEY," +
                    "multimedia_id INTEGER DEFAULT 0," +
                    "username VARCHAR(50) NOT NULL," +
                    "multimedia_titulo VARCHAR(150)," +
                    "multimedia_tipo VARCHAR(20)," +
                    "multimedia_anio INTEGER," +
                    "multimedia_genero VARCHAR(50)," +
                    "multimedia_descripcion TEXT," +
                    "multimedia_poster TEXT," +
                    "multimedia_streaming_url TEXT," +
                    "fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (username) REFERENCES usuarios(username)" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS foro_comentarios (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "post_id INTEGER NOT NULL," +
                    "username VARCHAR(50) NOT NULL," +
                    "texto VARCHAR(240) NOT NULL," +
                    "fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (post_id) REFERENCES foro_posts(id)," +
                    "FOREIGN KEY (username) REFERENCES usuarios(username)" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS foro_votos (" +
                    "post_id INTEGER NOT NULL," +
                    "username VARCHAR(50) NOT NULL," +
                    "is_like INTEGER NOT NULL," +
                    "PRIMARY KEY (post_id, username)," +
                    "FOREIGN KEY (post_id) REFERENCES foro_posts(id)," +
                    "FOREIGN KEY (username) REFERENCES usuarios(username)" +
                    ")");

            runMigrations(conn);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void runMigrations(Connection conn) {
        String[] migrations = {
            "ALTER TABLE multimedia ADD COLUMN streaming_url TEXT",
            "ALTER TABLE foro_posts ADD COLUMN multimedia_id INTEGER DEFAULT 0",
            "ALTER TABLE foro_posts ADD COLUMN multimedia_streaming_url TEXT"
        };
        try (Statement stmt = conn.createStatement()) {
            for (String sql : migrations) {
                try {
                    stmt.execute(sql);
                } catch (SQLException e) {
                    // Ignorar si la columna ya existe
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static CatalogoPeliculas loadCatalog(String username) {
        String key = norm(username);
        CatalogoPeliculas cat = new CatalogoPeliculas();
        if (key.isEmpty()) return cat;

        String sql = "SELECT * FROM multimedia WHERE username = ?";
        int maxId = 0;
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String tipo = rs.getString("tipo");
                String titulo = rs.getString("titulo");
                int anio = rs.getInt("anio");
                String genero = rs.getString("genero");
                String descripcion = rs.getString("descripcion");
                String poster = rs.getString("poster_data_url");
                String streamUrl = rs.getString("streaming_url");
                int dur = rs.getInt("duracion_min");
                int temps = rs.getInt("temporadas");
                int epis = rs.getInt("episodios");

                Multimedia m;
                if ("Serie".equalsIgnoreCase(tipo)) {
                    m = new SerieM(id, titulo, anio, genero, descripcion, poster, streamUrl, temps);
                } else if ("Anime".equalsIgnoreCase(tipo)) {
                    m = new AnimeM(id, titulo, anio, genero, descripcion, poster, streamUrl, epis);
                } else {
                    m = new PeliculaM(id, titulo, anio, genero, descripcion, poster, streamUrl, dur);
                }
                cat.agregar(m);
                if (id > maxId) maxId = id;
            }
            cat.ajustarNextId(maxId + 1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cat;
    }

    public static void saveCatalog(String username, CatalogoPeliculas catalogo) {
        String key = norm(username);
        if (key.isEmpty() || catalogo == null) return;

        try (Connection conn = getConnection()) {
            String deleteSql = "DELETE FROM multimedia WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                pstmt.setString(1, key);
                pstmt.executeUpdate();
            }

            String insertSql = "INSERT INTO multimedia (id, username, tipo, titulo, anio, genero, descripcion, poster_data_url, streaming_url, duracion_min, temporadas, episodios) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (Multimedia m : catalogo.listar()) {
                    pstmt.setInt(1, m.getId());
                    pstmt.setString(2, key);
                    String tipo = (m instanceof SerieM) ? "Serie" : (m instanceof AnimeM ? "Anime" : "Pelicula");
                    int dur = (m instanceof PeliculaM) ? ((PeliculaM) m).getDuracionMin() : 0;
                    int temps = (m instanceof SerieM) ? ((SerieM) m).getTemporadas() : 0;
                    int epis = (m instanceof AnimeM) ? ((AnimeM) m).getEpisodios() : 0;

                    pstmt.setString(3, tipo);
                    pstmt.setString(4, m.getTitulo());
                    pstmt.setInt(5, m.getAnio());
                    pstmt.setString(6, m.getGenero());
                    pstmt.setString(7, m.getDescripcion());
                    pstmt.setString(8, m.getPosterDataUrl());
                    pstmt.setString(9, m.getStreamingUrl());
                    pstmt.setInt(10, dur);
                    pstmt.setInt(11, temps);
                    pstmt.setInt(12, epis);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<ForumPost> loadForumPosts() {
        List<ForumPost> out = new ArrayList<>();
        String sql = "SELECT * FROM foro_posts";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new ForumPost(
                    rs.getInt("id"),
                    rs.getInt("multimedia_id"),
                    rs.getString("username"),
                    rs.getString("multimedia_titulo"),
                    rs.getString("multimedia_tipo"),
                    rs.getInt("multimedia_anio"),
                    rs.getString("multimedia_genero"),
                    rs.getString("multimedia_descripcion"),
                    rs.getString("multimedia_poster"),
                    rs.getString("multimedia_streaming_url")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    public static void saveForumPosts(List<ForumPost> posts) {
        if (posts == null) return;
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM foro_posts");
            }
            String sql = "INSERT INTO foro_posts (id, multimedia_id, username, multimedia_titulo, multimedia_tipo, multimedia_anio, multimedia_genero, multimedia_descripcion, multimedia_poster, multimedia_streaming_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (ForumPost p : posts) {
                    pstmt.setInt(1, p.getId());
                    pstmt.setInt(2, p.getMultimediaId());
                    pstmt.setString(3, norm(p.getUsuario()));
                    pstmt.setString(4, p.getTitulo());
                    pstmt.setString(5, p.getTipo());
                    pstmt.setInt(6, p.getAnio());
                    pstmt.setString(7, p.getGenero());
                    pstmt.setString(8, p.getDescripcion());
                    pstmt.setString(9, p.getPosterDataUrl());
                    pstmt.setString(10, p.getStreamingUrl());
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    public static Map<Integer, List<ForumComment>> loadForumComments() {
        Map<Integer, List<ForumComment>> map = new HashMap<>();
        String sql = "SELECT * FROM foro_comentarios";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int postId = rs.getInt("post_id");
                ForumComment fc = new ForumComment(
                    postId,
                    rs.getString("username"),
                    rs.getString("texto")
                );
                map.computeIfAbsent(postId, k -> new ArrayList<>()).add(fc);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static void saveForumComments(Map<Integer, List<ForumComment>> map) {
        if (map == null) return;
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM foro_comentarios");
            }
            String insertSql = "INSERT INTO foro_comentarios (post_id, username, texto) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (Map.Entry<Integer, List<ForumComment>> e : map.entrySet()) {
                    int postId = e.getKey();
                    for (ForumComment c : e.getValue()) {
                        pstmt.setInt(1, postId);
                        pstmt.setString(2, norm(c.getUsuario()));
                        pstmt.setString(3, c.getTexto());
                        pstmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Map<Integer, int[]> loadForumVotes() {
        Map<Integer, int[]> map = new HashMap<>();
        String sql = "SELECT post_id, is_like, COUNT(*) as cnt FROM foro_votos GROUP BY post_id, is_like";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int postId = rs.getInt("post_id");
                int isLike = rs.getInt("is_like");
                int count = rs.getInt("cnt");
                map.putIfAbsent(postId, new int[]{0, 0});
                if (isLike == 1) {
                    map.get(postId)[0] = count;
                } else if (isLike == -1) {
                    map.get(postId)[1] = count;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static void saveForumVote(int postId, String username, int isLike) {
        String key = norm(username);
        if (key.isEmpty()) return;
        try (Connection conn = getConnection()) {
            if (isLike == 0) {
                String delSql = "DELETE FROM foro_votos WHERE post_id = ? AND username = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(delSql)) {
                    pstmt.setInt(1, postId);
                    pstmt.setString(2, key);
                    pstmt.executeUpdate();
                }
            } else {
                String upsertSql = "INSERT OR REPLACE INTO foro_votos (post_id, username, is_like) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(upsertSql)) {
                    pstmt.setInt(1, postId);
                    pstmt.setString(2, key);
                    pstmt.setInt(3, isLike == 1 ? 1 : -1);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String norm(String username) {
        if (username == null) return "";
        return username.trim().toLowerCase();
    }

    public static void deleteForumPost(int postId) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM foro_votos WHERE post_id = ?")) {
                pstmt.setInt(1, postId);
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM foro_comentarios WHERE post_id = ?")) {
                pstmt.setInt(1, postId);
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM foro_posts WHERE id = ?")) {
                pstmt.setInt(1, postId);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static void updateForumPostSnapshot(int multimediaId, String username, Multimedia m) {
        String sql = "UPDATE foro_posts SET multimedia_titulo = ?, multimedia_tipo = ?, multimedia_anio = ?, multimedia_genero = ?, multimedia_descripcion = ?, multimedia_poster = ?, multimedia_streaming_url = ? WHERE multimedia_id = ? AND username = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, m.getTitulo());
            pstmt.setString(2, (m instanceof SerieM) ? "Serie" : (m instanceof AnimeM ? "Anime" : "Pelicula"));
            pstmt.setInt(3, m.getAnio());
            pstmt.setString(4, m.getGenero());
            pstmt.setString(5, m.getDescripcion());
            pstmt.setString(6, m.getPosterDataUrl());
            pstmt.setString(7, m.getStreamingUrl());
            pstmt.setInt(8, multimediaId);
            pstmt.setString(9, norm(username));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
     }

    public static void exportarCSV(List<Multimedia> catalog, String filePath) {
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(filePath);
             java.io.PrintWriter writer = new java.io.PrintWriter(
                 new java.io.OutputStreamWriter(fos, java.nio.charset.StandardCharsets.UTF_8))) {
            writer.println("ID,Type,Title,Year,Genre");
            for (Multimedia m : catalog) {
                String tipo = (m instanceof SerieM) ? "Serie" : (m instanceof AnimeM) ? "Anime" : "Pelicula";
                writer.printf("%d,%s,\"%s\",%d,\"%s\"%n",
                    m.getId(), tipo,
                    m.getTitulo().replace("\"", "\"\""),
                    m.getAnio(),
                    m.getGenero().replace("\"", "\"\""));
            }
        } catch (Exception e) {
            throw new CineCritikException("Export error: " + e.getMessage());
        }
    }
}

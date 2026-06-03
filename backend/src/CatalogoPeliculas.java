import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bloque 4 y 11: Gestión de colecciones + estructura de datos.
 * Contiene una lista de Multimedia (polimórfica).
 */
public class CatalogoPeliculas {
  // Bloque 11: colección (lista)
  private final List<Multimedia> items = new ArrayList<Multimedia>();
  private int nextId = 1;

  public int nextId() {
    return nextId++;
  }

  // Ajusta el siguiente ID después de cargar desde archivo.
  public void ajustarNextId(int siguiente) {
    if (siguiente > 0) {
      this.nextId = siguiente;
    }
  }

  public void agregar(Multimedia m) {
    items.add(m);
  }

  public boolean actualizar(Multimedia m) {
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).getId() == m.getId()) {
        items.set(i, m);
        return true;
      }
    }
    return false;
  }

  public boolean eliminarPorId(int id) {
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).getId() == id) {
        items.remove(i);
        return true;
      }
    }
    return false;
  }

  public List<Multimedia> listar() {
    return Collections.unmodifiableList(items);
  }

  public Multimedia buscarPorId(int id) {
    for (Multimedia m : items) {
      if (m.getId() == id) return m;
    }
    return null;
  }

  static class Creador {
    private final CatalogoPeliculas catalogo;

    public Creador(CatalogoPeliculas c) {
      this.catalogo = c;
    }

    public PeliculaM crearPelicula(String titulo, int anio, String genero, String descripcion, String posterDataUrl, String streamingUrl, int duracionMin) {
      int id = catalogo.nextId();
      PeliculaM p = new PeliculaM(id, titulo, anio, genero, descripcion, posterDataUrl, streamingUrl, duracionMin);
      catalogo.agregar(p);
      return p;
    }

    public SerieM crearSerie(String titulo, int anio, String genero, String descripcion, String posterDataUrl, String streamingUrl, int temporadas) {
      int id = catalogo.nextId();
      SerieM s = new SerieM(id, titulo, anio, genero, descripcion, posterDataUrl, streamingUrl, temporadas);
      catalogo.agregar(s);
      return s;
    }

    public AnimeM crearAnime(String titulo, int anio, String genero, String descripcion, String posterDataUrl, String streamingUrl, int episodios) {
      int id = catalogo.nextId();
      AnimeM a = new AnimeM(id, titulo, anio, genero, descripcion, posterDataUrl, streamingUrl, episodios);
      catalogo.agregar(a);
      return a;
    }

    // Para edición: conserva el ID
    public Multimedia crearConId(int id, String tipo, String titulo, int anio, String genero, String descripcion, String posterDataUrl, String streamingUrl, int duracionMin, int temporadas, int episodios) {
      if ("Serie".equalsIgnoreCase(tipo)) {
        return new SerieM(id, titulo, anio, genero, descripcion, posterDataUrl, streamingUrl, temporadas);
      }
      if ("Anime".equalsIgnoreCase(tipo)) {
        return new AnimeM(id, titulo, anio, genero, descripcion, posterDataUrl, streamingUrl, episodios);
      }
      return new PeliculaM(id, titulo, anio, genero, descripcion, posterDataUrl, streamingUrl, duracionMin);
    }
  }
}


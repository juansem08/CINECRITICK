public abstract class Espectador {
  protected String nombre;
  protected String nivelCinefilo;

  protected Espectador(String nombre) {
    this.nombre = nombre;
    this.nivelCinefilo = "PRINCIPIANTE";
  }

  public String getNombre() {
    return nombre;
  }

  public String getNivelCinefilo() {
    return nivelCinefilo;
  }

  public abstract String darOpinion();
}

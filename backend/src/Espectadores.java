class FanaticoCine extends Espectador implements Revisable {
  public FanaticoCine(String nombre) {
    super(nombre);
    this.nivelCinefilo = "CINEFILO";
  }

  @Override
  public String darOpinion() {
    return "Se emociona muchísimo con las referencias intertextuales a otros universos.";
  }

  @Override
  public String analizarPelicula() {
    return darOpinion();
  }

  @Override
  public int calificarRecomendacion() {
    return 5; // Fácil de complacer
  }
}

class UsuarioCasual extends Espectador implements Revisable {
  public UsuarioCasual(String nombre) {
    super(nombre);
    this.nivelCinefilo = "ESPECTADOR_CASUAL";
  }

  @Override
  public String darOpinion() {
    return "Ve la película mientras usa el celular, se ríe en las partes equivocadas.";
  }

  @Override
  public String analizarPelicula() {
    return darOpinion();
  }

  @Override
  public int calificarRecomendacion() {
    return 3; // Neutral
  }
}

class CriticoExperto extends Espectador implements Revisable {
  public CriticoExperto(String nombre) {
    super(nombre);
    this.nivelCinefilo = "EXPERTO";
  }

  @Override
  public String darOpinion() {
    return "Analiza exhaustivamente el guion, la iluminación y la paleta de colores de la cinta.";
  }

  @Override
  public String analizarPelicula() {
    return darOpinion();
  }

  @Override
  public int calificarRecomendacion() {
    return 4; // Opinión exigente
  }
}

class ArticuloDeCine implements Revisable {
  @Override
  public String analizarPelicula() {
    return "Trama muy profunda con giros argumentales espectaculares y una fotografía premiada.";
  }

  @Override
  public int calificarRecomendacion() {
    return 5; // Puntaje máximo 5 estrellas
  }
}

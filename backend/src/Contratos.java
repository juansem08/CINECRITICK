interface Calificable {
    void calificar(int puntaje);
    double getPromedio();
    int getTotalVotos();
}

interface Revisable {
  String analizarPelicula();
  int calificarRecomendacion();
}

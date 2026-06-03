## CineCritik — Proyecto Final Ingeniería de Diseño de Software

Demo inmersiva **Java + SPA** que cubre los Bloques 3–14 del curso:
POO, herencia, polimorfismo, abstracción, encapsulamiento, interfaces,
análisis OO, componentes, I/O, excepciones y principios SOLID.
Incluye demo adicional con **Spring Boot + MongoDB Atlas**.

---

## Diagramas 

<img width="1384" height="499" alt="Captura de pantalla 2026-06-03 103240" src="https://github.com/user-attachments/assets/5872bde2-7199-40f0-ad9a-dab872f2b946" />
<img width="1659" height="217" alt="Captura de pantalla 2026-06-03 102822" src="https://github.com/user-attachments/assets/cc5155ff-a352-4366-8440-45d50b8ebda6" />
<img width="432" height="551" alt="Captura de pantalla 2026-06-03 115040" src="https://github.com/user-attachments/assets/a4d97016-f125-4cdc-a6a1-e1f2d8b5fd1f" />

## Vista de contexto
<img width="1010" height="458" alt="Captura de pantalla 2026-06-03 111738" src="https://github.com/user-attachments/assets/dfffe75a-d1ea-4995-b058-1c460db535c6" />
<img width="1632" height="616" alt="Captura de pantalla 2026-06-03 110804" src="https://github.com/user-attachments/assets/c100a015-dedf-4746-9ad3-63bfd622957b" />

## Modelo conceptual de clases (Vista lógica)

<img width="1610" height="705" alt="Captura de pantalla 2026-06-03 105948" src="https://github.com/user-attachments/assets/086e7a85-bdde-4a75-b555-74594edb7bfd" />

-----

### Estructura del proyecto
protocolo-los-marcados/
├── config.properties          ← credenciales del admin
├── data/                      ← SQLite (se regenera automáticamente)
├── backend/
│   ├── lib/sqlite-jdbc.jar
│   └── src/                   ← 13 archivos .java
├── frontend/                  ← SPA (index.html, app.js, styles.css)
└── spring-demo/               ← demo Spring Boot + MongoDB (puerto 8080)

---

### Requisitos

- **JDK 11+** con `javac` en el PATH
- **Maven 3.6+** para el spring-demo (`mvn -version` para verificar)

---

### Compilar y ejecutar el backend principal

Desde la carpeta `protocolo-los-marcados/`:

```powershell
javac -cp backend/lib/sqlite-jdbc.jar -d backend/out backend/src/*.java
java -cp "backend/out;backend/lib/sqlite-jdbc.jar" Main
```

Abrir en el navegador: `http://127.0.0.1:7071`

---

### Ejecutar el Spring Demo (MongoDB)

Desde la carpeta `spring-demo/`:

```powershell
mvn spring-boot:run
```

Corre en: `http://localhost:8080`

Ambos backends pueden correr simultáneamente.

---

### Credenciales

- Usuario admin: `andres` / Contraseña: `cine`
- Los demás usuarios se registran desde la misma app

---

### Endpoints — Backend principal (puerto 7071)

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| POST | `/api/login` | No | Autenticación, retorna token UUID |
| POST | `/api/register` | No | Registro de nuevo usuario |
| GET | `/api/catalogo` | Sí | Lista el catálogo del usuario |
| POST | `/api/catalogo/agregar` | Sí | Crea Pelicula / Serie / Anime |
| POST | `/api/catalogo/editar` | Sí | Edita elemento por ID |
| POST | `/api/catalogo/eliminar` | Sí | Elimina elemento por ID |
| POST | `/api/catalogo/calificar` | Sí | Califica elemento (1–5) |
| GET | `/api/catalogo/exportar` | Sí | Exporta catálogo a CSV |
| GET | `/api/info` | Sí | Polimorfismo: Espectadores + ArticuloDeCine |
| POST | `/api/forum/publicar` | Sí | Publica elemento en el foro global |
| GET | `/api/forum/list` | No | Lista posts del foro con likes/dislikes |
| POST | `/api/forum/vote` | Sí | Like o dislike en un post |
| POST | `/api/forum/comment` | Sí | Comenta un post (máx 240 chars) |
| GET | `/api/forum/comments` | No | Lista comentarios de un post |
| POST | `/api/forum/delete` | Sí | Elimina post propio |

---

### Endpoints — Spring Demo (puerto 8080)

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/spring/catalogo` | Lista todos los documentos en MongoDB |
| POST | `/spring/catalogo` | Guarda un documento nuevo |
| GET | `/spring/catalogo/tipo/{tipo}` | Filtra por Pelicula / Serie / Anime |
| GET | `/spring/catalogo/{id}` | Busca documento por ID |
| DELETE | `/spring/catalogo/{id}` | Elimina documento por ID |

---

### Mapa de bloques del curso

| Bloque | Tema | Implementación |
|--------|------|----------------|
| 3 | Tipos de programación | Arquitectura completa Java |
| 4 | Clases y objetos | `Multimedia`, `CatalogoPeliculas`, `ForumPost` |
| 5–6 | Herencia | `Multimedia → PeliculaM/SerieM/AnimeM`, `Espectador → subclases` |
| 7 | Polimorfismo | `darOpinion()`, `ejecutarAccion()`, `mensajeMetraje()` |
| 8 | Clases abstractas | `abstract Multimedia`, `abstract Espectador` |
| 9 | Encapsulamiento | `private` en `GestorAcceso`, `RepositorioUsuarios` |
| 10 | Interfaces | `Calificable`, `Revisable`, `Autenticable` |
| 11 | Análisis OO | Diagramas UML (clases, secuencia, componentes) |
| 12 | Componentes | Diagrama de componentes del sistema |
| 13 | I/O y Excepciones | `DataStore.exportarCSV()`, `CineCritikException` |
| 14 | SOLID | `SOLID_CineCritik`, `ValidadorEntrada`, `Autenticable` |

---

### Principios SOLID aplicados

- **S** — SRP: `DataStore` solo persiste, `RepositorioUsuarios` solo gestiona usuarios
- **O** — OCP: nuevo tipo multimedia = nueva subclase de `Multimedia`, sin tocar el resto
- **L** — LSP: `PeliculaM`, `SerieM`, `AnimeM` sustituyen a `Multimedia` en cualquier `List<Multimedia>`
- **I** — ISP: interfaces pequeñas (`Calificable`, `Revisable`, `Autenticable`)
- **D** — DIP: `Main` depende de abstracciones, no de implementaciones concretas

---

### Jerarquía de clases
Multimedia (abstract) implements Calificable
├── PeliculaM
├── SerieM
└── AnimeM
Espectador (abstract)
├── FanaticoCine   implements Revisable
├── UsuarioCasual  implements Revisable
└── CriticoExperto implements Revisable
ArticuloDeCine implements Revisable
SOLID_CineCritik
├── interface Autenticable
└── GestorAcceso implements Autenticable
CatalogoPeliculas
└── inner class Creador

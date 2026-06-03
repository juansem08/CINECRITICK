## Protocolo Los Marcados

Demo inmersiva **Java + SPA** para POO (Bloques 8, 9, 10 y herencia/polimorfismo).
Extendido para Bloques **4–11** con **Catálogo**, **Clase de creación** y **Galería**.

### Requisitos
- **JDK 8+** (necesitas `javac` en el PATH).  
  Nota: si solo tienes `java` y no `javac`, instalaste un **JRE** y no un **JDK**.

### Compilar y ejecutar
En PowerShell, dentro de esta carpeta:

```powershell
javac -d out src\*.java
java -cp out Main
```

Luego abre en el navegador:
- `http://127.0.0.1:7071`

### Credenciales
- Usuario: `andres`
- Contraseña: `marcados`

### Endpoints
- `POST /api/login` → valida contra `GestorAcceso` (Bloque 9: `private`).
- `GET /api/catalogo` → lista `Multimedia[]` (Bloques 4/11: colección).
- `POST /api/catalogo/agregar` → crea `PeliculaM` o `SerieM` (Bloques 4/11: clase de creación).
- `GET /api/info` → devuelve `catalogo`, evidencia `Investigable` y sujetos (con `protected estadoMarcado` + reacción polimórfica).


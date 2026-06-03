package com.cinecritik.controller;

import com.cinecritik.model.MultimediaDocument;
import com.cinecritik.service.MultimediaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/spring/catalogo")
@CrossOrigin(origins = "*") // Permite al frontend principal consumir estos endpoints
public class CatalogoController {

    private final MultimediaService service;

    // Inyección de dependencias del Service Layer mediante constructor
    public CatalogoController(MultimediaService service) {
        this.service = service;
    }

    // 1. GET /spring/catalogo -> Devuelve todos los documentos
    @GetMapping
    public List<MultimediaDocument> getAll() {
        return service.obtenerTodos();
    }

    // 2. POST /spring/catalogo -> Crea/guarda un documento con validaciones del service
    @PostMapping
    public ResponseEntity<?> create(@RequestBody MultimediaDocument document) {
        try {
            MultimediaDocument saved = service.guardar(document);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 3. GET /spring/catalogo/tipo/{tipo} -> Filtra por tipo (normalizado por el service)
    @GetMapping("/tipo/{tipo}")
    public List<MultimediaDocument> getByTipo(@PathVariable String tipo) {
        return service.obtenerPorTipo(tipo);
    }

    // 4. GET /spring/catalogo/{id} -> Busca un documento por _id
    @GetMapping("/{id}")
    public ResponseEntity<MultimediaDocument> getById(@PathVariable String id) {
        Optional<MultimediaDocument> doc = service.obtenerPorId(id);
        return doc.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 5. DELETE /spring/catalogo/{id} -> Elimina un documento por _id
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteById(@PathVariable String id) {
        boolean ok = service.eliminarPorId(id);
        if (ok) {
            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("deleted", id);
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 6. GET /spring/catalogo/recientes -> Lógica de negocio de películas recomendadas recientes
    @GetMapping("/recientes")
    public List<MultimediaDocument> getRecientes() {
        return service.obtenerRecomendadasRecientes();
    }

    // 7. GET /spring/catalogo/contar/{tipo} -> Cuenta los elementos registrados por tipo
    @GetMapping("/contar/{tipo}")
    public ResponseEntity<Map<String, Object>> countByTipo(@PathVariable String tipo) {
        long count = service.contarPorTipo(tipo);
        Map<String, Object> response = new HashMap<>();
        response.put("tipo", tipo);
        response.put("cantidad", count);
        return ResponseEntity.ok(response);
    }
}

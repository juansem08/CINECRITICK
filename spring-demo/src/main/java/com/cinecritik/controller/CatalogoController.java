package com.cinecritik.controller;

import com.cinecritik.model.MultimediaDocument;
import com.cinecritik.repository.MultimediaRepository;
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

    private final MultimediaRepository repository;

    // Inyección de dependencias mediante constructor (Buenas prácticas de diseño)
    public CatalogoController(MultimediaRepository repository) {
        this.repository = repository;
    }

    // 1. GET /spring/catalogo -> Devuelve todos los documentos de la colección
    @GetMapping
    public List<MultimediaDocument> getAll() {
        return repository.findAll();
    }

    // 2. POST /spring/catalogo -> Recibe un documento y lo guarda en MongoDB
    @PostMapping
    public MultimediaDocument create(@RequestBody MultimediaDocument document) {
        return repository.save(document);
    }

    // 3. GET /spring/catalogo/tipo/{tipo} -> Filtra por tipo (Pelicula, Serie, Anime)
    @GetMapping("/tipo/{tipo}")
    public List<MultimediaDocument> getByTipo(@PathVariable String tipo) {
        return repository.findByTipo(tipo);
    }

    // 4. GET /spring/catalogo/{id} -> Busca un documento por _id
    @GetMapping("/{id}")
    public ResponseEntity<MultimediaDocument> getById(@PathVariable String id) {
        Optional<MultimediaDocument> doc = repository.findById(id);
        if (doc.isPresent()) {
            return ResponseEntity.ok(doc.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 5. DELETE /spring/catalogo/{id} -> Elimina un documento por _id
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteById(@PathVariable String id) {
        Optional<MultimediaDocument> doc = repository.findById(id);
        if (doc.isPresent()) {
            repository.deleteById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("deleted", id);
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

package kh.edu.cstad.devops.controller;

import kh.edu.cstad.devops.model.TaskItem;
import kh.edu.cstad.devops.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class TaskController {

    private final TaskRepository taskRepository;

    @Value("${app.environment:production}")
    private String environment;

    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        String hostname = "unknown";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("service", "cstad-devops-backend");
        payload.put("instructor", "Mr. Ing Muyleang");
        payload.put("environment", environment);
        payload.put("hostname", hostname);
        payload.put("database", "PostgreSQL Connected (" + taskRepository.count() + " records)");
        return payload;
    }

    @GetMapping("/tasks")
    public List<TaskItem> listTasks() {
        return taskRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @PostMapping("/tasks")
    public ResponseEntity<TaskItem> createTask(@RequestBody TaskItem item) {
        if (item.getTitle() == null || item.getTitle().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        TaskItem saved = taskRepository.save(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/tasks/{id}")
    public ResponseEntity<TaskItem> updateTask(@PathVariable Long id, @RequestBody TaskItem incoming) {
        return taskRepository.findById(id)
                .map(existing -> {
                    if (incoming.getTitle() != null && !incoming.getTitle().isBlank()) {
                        existing.setTitle(incoming.getTitle());
                    }
                    if (incoming.getOwner() != null && !incoming.getOwner().isBlank()) {
                        existing.setOwner(incoming.getOwner());
                    }
                    if (incoming.getStatus() != null && !incoming.getStatus().isBlank()) {
                        existing.setStatus(incoming.getStatus());
                    }
                    return ResponseEntity.ok(taskRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        if (!taskRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        taskRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}


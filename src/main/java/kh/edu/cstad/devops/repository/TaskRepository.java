package kh.edu.cstad.devops.repository;

import kh.edu.cstad.devops.model.TaskItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<TaskItem, Long> {
}


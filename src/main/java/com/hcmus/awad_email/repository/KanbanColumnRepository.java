package com.hcmus.awad_email.repository;

import com.hcmus.awad_email.model.KanbanColumn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KanbanColumnRepository extends MongoRepository<KanbanColumn, String> {
    
    List<KanbanColumn> findByUserIdOrderByOrderAsc(String userId);
    
    Optional<KanbanColumn> findByIdAndUserId(String id, String userId);
    
    Optional<KanbanColumn> findByUserIdAndType(String userId, KanbanColumn.ColumnType type);
    
    boolean existsByUserIdAndName(String userId, String name);
    
    long countByUserId(String userId);
    
    void deleteByIdAndUserId(String id, String userId);
}


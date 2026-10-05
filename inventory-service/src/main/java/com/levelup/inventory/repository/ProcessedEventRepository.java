package com.levelup.inventory.repository;

import com.levelup.inventory.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository  extends JpaRepository<ProcessedEvent, UUID> {
}

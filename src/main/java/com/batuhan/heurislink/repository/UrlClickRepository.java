package com.batuhan.heurislink.repository;

import com.batuhan.heurislink.entity.UrlClick;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UrlClickRepository extends JpaRepository<UrlClick, UUID> {
    long countByShortUrlId(UUID shortUrlId);
}
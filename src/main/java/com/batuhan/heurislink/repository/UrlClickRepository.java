package com.batuhan.heurislink.repository;

import com.batuhan.heurislink.entity.UrlClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface UrlClickRepository extends JpaRepository<UrlClick, UUID> {

    long countByShortUrlId(UUID shortUrlId);

    @Modifying
    @Query(
            value = """
                    INSERT INTO url_clicks (
                        id,
                        event_id,
                        short_url_id,
                        clicked_at
                    )
                    VALUES (
                        :id,
                        :eventId,
                        :shortUrlId,
                        :clickedAt
                    )
                    ON CONFLICT (event_id) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("eventId") UUID eventId,
            @Param("shortUrlId") UUID shortUrlId,
            @Param("clickedAt") LocalDateTime clickedAt
    );
}
package com.thang.roombooking.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "translations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"entity_type", "entity_id", "locale", "field_name"})
})
public class Translation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // e.g., 'CLASSROOM', 'BUILDING', 'EQUIPMENT'

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "locale", nullable = false, length = 5)
    private String locale; // e.g., 'vi', 'en'

    @Column(name = "field_name", nullable = false, length = 50)
    private String fieldName; // e.g., 'description', 'name'

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
}

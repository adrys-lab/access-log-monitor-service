package com.adrian.rebollo.mapper;

import java.util.Optional;

/**
 * Interface for bi-directional mappings K <-> T.
 *
 * @param <K> First arg used as entity.
 * @param <T> Second arg used as domain.
 */
public interface BiMapper<K, T> {

    default Optional<T> toDomain(Optional<K> entity) {
        return entity.map(this::toDomain)
                .or(Optional::empty);
    }

    /**
     * @param entity to map as domain
     *
     * @return domain
     */
    T toDomain(K entity);

    /**
     * @param domain to map as entity
     *
     * @return entity
     */
    K toEntity(T domain);

}

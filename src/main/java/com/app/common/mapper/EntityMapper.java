package com.app.common.mapper;

import java.util.List;

public interface EntityMapper<D, E>{

    E toEntity(D dto);

    D toDto(E entity);

    List<E> toEntity(List<D> dTos);

    List<D> toDto(List<E> eTos);


}

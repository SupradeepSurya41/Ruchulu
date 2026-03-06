package com.ruchulu.catererservice.repository;

import com.ruchulu.catererservice.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, String> {

    List<MenuItem> findByCaterer_IdAndIsAvailableTrue(String catererId);
    List<MenuItem> findByCaterer_Id(String catererId);
    List<MenuItem> findByCaterer_IdAndCategory(String catererId, MenuCategory category);
    List<MenuItem> findByCaterer_IdAndIsVegetarianTrue(String catererId);

    @Query("""
        SELECT m FROM MenuItem m
        WHERE m.caterer.id = :catererId
        AND (LOWER(m.name) LIKE LOWER(CONCAT('%',:q,'%'))
          OR LOWER(m.description) LIKE LOWER(CONCAT('%',:q,'%')))
    """)
    List<MenuItem> searchMenuItems(@Param("catererId") String catererId, @Param("q") String query);

    boolean existsByIdAndCaterer_Id(String id, String catererId);
    Optional<MenuItem> findByIdAndCaterer_Id(String id, String catererId);
    long countByCaterer_Id(String catererId);
}

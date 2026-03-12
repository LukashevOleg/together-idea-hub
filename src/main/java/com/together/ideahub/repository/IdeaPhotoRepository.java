package com.together.ideahub.repository;

import com.together.ideahub.entity.IdeaPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IdeaPhotoRepository extends JpaRepository<IdeaPhoto, Long> {
    List<IdeaPhoto> findByIdeaIdOrderBySortOrderAsc(Long ideaId);
    void deleteByIdeaId(Long ideaId);
}
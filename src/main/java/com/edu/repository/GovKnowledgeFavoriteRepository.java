package com.edu.repository;

import com.edu.pojo.po.gov.GovKnowledgeFavoritePO;

import java.util.List;

public interface GovKnowledgeFavoriteRepository {
    GovKnowledgeFavoritePO selectFavorite(Long userId, Long knowledgeId);

    GovKnowledgeFavoritePO selectRecord(Long userId, Long knowledgeId);

    List<GovKnowledgeFavoritePO> selectFavoritesByUser(Long userId);

    int insertFavorite(GovKnowledgeFavoritePO favorite);

    int updateFavorite(GovKnowledgeFavoritePO favorite);

    int logicalDeleteFavorite(Long userId, Long knowledgeId);
}

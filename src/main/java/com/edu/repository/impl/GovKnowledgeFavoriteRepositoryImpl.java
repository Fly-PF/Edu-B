package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.edu.mapper.gov.GovKnowledgeFavoriteMapper;
import com.edu.pojo.po.gov.GovKnowledgeFavoritePO;
import com.edu.repository.GovKnowledgeFavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class GovKnowledgeFavoriteRepositoryImpl implements GovKnowledgeFavoriteRepository {
    private final GovKnowledgeFavoriteMapper govKnowledgeFavoriteMapper;

    @Override
    public GovKnowledgeFavoritePO selectFavorite(Long userId, Long knowledgeId) {
        return govKnowledgeFavoriteMapper.selectOne(new LambdaQueryWrapper<GovKnowledgeFavoritePO>()
                .eq(GovKnowledgeFavoritePO::getUserId, userId)
                .eq(GovKnowledgeFavoritePO::getKnowledgeId, knowledgeId)
                .eq(GovKnowledgeFavoritePO::getDeleted, 0));
    }

    @Override
    public GovKnowledgeFavoritePO selectRecord(Long userId, Long knowledgeId) {
        return govKnowledgeFavoriteMapper.selectOne(new LambdaQueryWrapper<GovKnowledgeFavoritePO>()
                .eq(GovKnowledgeFavoritePO::getUserId, userId)
                .eq(GovKnowledgeFavoritePO::getKnowledgeId, knowledgeId));
    }

    @Override
    public List<GovKnowledgeFavoritePO> selectFavoritesByUser(Long userId) {
        return govKnowledgeFavoriteMapper.selectList(new LambdaQueryWrapper<GovKnowledgeFavoritePO>()
                .eq(GovKnowledgeFavoritePO::getUserId, userId)
                .eq(GovKnowledgeFavoritePO::getDeleted, 0)
                .orderByDesc(GovKnowledgeFavoritePO::getUpdateTime)
                .orderByDesc(GovKnowledgeFavoritePO::getId));
    }

    @Override
    public int insertFavorite(GovKnowledgeFavoritePO favorite) {
        return govKnowledgeFavoriteMapper.insert(favorite);
    }

    @Override
    public int updateFavorite(GovKnowledgeFavoritePO favorite) {
        return govKnowledgeFavoriteMapper.update(favorite, new LambdaUpdateWrapper<GovKnowledgeFavoritePO>()
                .eq(GovKnowledgeFavoritePO::getId, favorite.getId()));
    }

    @Override
    public int logicalDeleteFavorite(Long userId, Long knowledgeId) {
        return govKnowledgeFavoriteMapper.update(null, new LambdaUpdateWrapper<GovKnowledgeFavoritePO>()
                .eq(GovKnowledgeFavoritePO::getUserId, userId)
                .eq(GovKnowledgeFavoritePO::getKnowledgeId, knowledgeId)
                .eq(GovKnowledgeFavoritePO::getDeleted, 0)
                .set(GovKnowledgeFavoritePO::getDeleted, 1)
                .set(GovKnowledgeFavoritePO::getUpdateTime, LocalDateTime.now()));
    }
}

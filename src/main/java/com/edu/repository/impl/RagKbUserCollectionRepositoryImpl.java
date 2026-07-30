package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.edu.mapper.RagKbUserCollectionMapper;
import com.edu.pojo.po.RagKbUserCollectionPO;
import com.edu.repository.RagKbUserCollectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RagKbUserCollectionRepositoryImpl implements RagKbUserCollectionRepository {
    private final RagKbUserCollectionMapper ragKbUserCollectionMapper;

    @Override
    public RagKbUserCollectionPO selectCollection(Long userId, Long kbId) {
        return ragKbUserCollectionMapper.selectOne(new LambdaQueryWrapper<RagKbUserCollectionPO>()
                .eq(RagKbUserCollectionPO::getUserId, userId)
                .eq(RagKbUserCollectionPO::getKbId, kbId)
                .last("limit 1"));
    }

    @Override
    public boolean existsActiveCollection(Long userId, Long kbId) {
        return ragKbUserCollectionMapper.exists(new LambdaQueryWrapper<RagKbUserCollectionPO>()
                .eq(RagKbUserCollectionPO::getUserId, userId)
                .eq(RagKbUserCollectionPO::getKbId, kbId)
                .eq(RagKbUserCollectionPO::getDeleted, 0));
    }

    @Override
    public int insertCollection(RagKbUserCollectionPO collection) {
        return ragKbUserCollectionMapper.insert(collection);
    }

    @Override
    public int restoreCollection(Long id) {
        return ragKbUserCollectionMapper.update(new LambdaUpdateWrapper<RagKbUserCollectionPO>()
                .eq(RagKbUserCollectionPO::getId, id)
                .set(RagKbUserCollectionPO::getDeleted, 0));
    }

    @Override
    public int cancelCollection(Long userId, Long kbId) {
        return ragKbUserCollectionMapper.update(new LambdaUpdateWrapper<RagKbUserCollectionPO>()
                .eq(RagKbUserCollectionPO::getUserId, userId)
                .eq(RagKbUserCollectionPO::getKbId, kbId)
                .eq(RagKbUserCollectionPO::getDeleted, 0)
                .set(RagKbUserCollectionPO::getDeleted, 1));
    }
}

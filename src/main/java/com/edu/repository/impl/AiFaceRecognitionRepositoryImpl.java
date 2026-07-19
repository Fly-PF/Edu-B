package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.mapper.AiFaceCompareRecordMapper;
import com.edu.mapper.AiFaceProfileMapper;
import com.edu.pojo.po.AiFaceCompareRecordPO;
import com.edu.pojo.po.AiFaceProfilePO;
import com.edu.repository.AiFaceRecognitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AiFaceRecognitionRepositoryImpl implements AiFaceRecognitionRepository {
    private static final int NOT_DELETED = 0;

    private final AiFaceProfileMapper faceProfileMapper;
    private final AiFaceCompareRecordMapper faceCompareRecordMapper;

    @Override
    public AiFaceProfilePO selectProfileByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return faceProfileMapper.selectOne(new LambdaQueryWrapper<AiFaceProfilePO>()
                .eq(AiFaceProfilePO::getUserId, userId)
                .eq(AiFaceProfilePO::getDeleted, NOT_DELETED));
    }

    @Override
    public int insertProfile(AiFaceProfilePO profile) {
        return faceProfileMapper.insert(profile);
    }

    @Override
    public int updateProfile(AiFaceProfilePO profile) {
        return faceProfileMapper.updateById(profile);
    }

    @Override
    public int insertCompareRecord(AiFaceCompareRecordPO record) {
        return faceCompareRecordMapper.insert(record);
    }

    @Override
    public IPage<AiFaceCompareRecordPO> selectComparePage(long pageNum, long pageSize, Long userId) {
        return faceCompareRecordMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<AiFaceCompareRecordPO>()
                        .eq(AiFaceCompareRecordPO::getDeleted, NOT_DELETED)
                        .eq(userId != null, AiFaceCompareRecordPO::getUserId, userId)
                        .orderByDesc(AiFaceCompareRecordPO::getCreateTime)
                        .orderByDesc(AiFaceCompareRecordPO::getId)
        );
    }
}

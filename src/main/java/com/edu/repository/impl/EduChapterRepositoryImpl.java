package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.mapper.EduChapterMapper;
import com.edu.pojo.po.EduChapterPO;
import com.edu.repository.EduChapterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EduChapterRepositoryImpl implements EduChapterRepository {
    private final EduChapterMapper eduChapterMapper;

    @Override
    public EduChapterPO selectChapterById(Long chapterId) {
        LambdaQueryWrapper<EduChapterPO> queryWrapper = new LambdaQueryWrapper<EduChapterPO>()
                .eq(EduChapterPO::getId, chapterId);
        return eduChapterMapper.selectOne(queryWrapper);
    }

    @Override
    public List<EduChapterPO> selectChaptersByCourseId(Long courseId) {
        LambdaQueryWrapper<EduChapterPO> queryWrapper = new LambdaQueryWrapper<EduChapterPO>()
                .eq(EduChapterPO::getCourseId, courseId)
                .orderByAsc(EduChapterPO::getSort);
        return eduChapterMapper.selectList(queryWrapper);
    }
}

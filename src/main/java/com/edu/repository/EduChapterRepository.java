package com.edu.repository;

import com.edu.pojo.po.EduChapterPO;

import java.util.List;

public interface EduChapterRepository {
    EduChapterPO selectChapterById(Long chapterId);

    List<EduChapterPO> selectChaptersByCourseId(Long courseId);
}

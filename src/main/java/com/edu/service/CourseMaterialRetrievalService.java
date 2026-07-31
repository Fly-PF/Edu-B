package com.edu.service;

import com.edu.pojo.vo.ai.AiCompanionMaterialExcerpt;
import com.edu.pojo.vo.course.ResourceVO;

import java.util.List;

public interface CourseMaterialRetrievalService {
    List<AiCompanionMaterialExcerpt> retrieve(List<ResourceVO> resources, String question);
}

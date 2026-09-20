package com.edu.service;

import com.edu.common.PageResult;
import com.edu.pojo.dto.skill.AiSkillCategorySaveRequest;
import com.edu.pojo.dto.skill.AiSkillInvokeRequest;
import com.edu.pojo.dto.skill.AiSkillSaveRequest;
import com.edu.pojo.vo.skill.AiSkillCategoryVO;
import com.edu.pojo.vo.skill.AiSkillInvokeVO;
import com.edu.pojo.vo.skill.AiSkillVO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AiSkillService {
    PageResult<AiSkillVO> pageMarket(String keyword, String skillType, String subjectType, String targetAudience,
                                     Long categoryId, Integer pageNum, Integer pageSize);

    PageResult<AiSkillVO> pageMySkills(String keyword, String status, Integer pageNum, Integer pageSize);

    AiSkillVO detail(Long skillId);

    AiSkillVO createDraft(AiSkillSaveRequest request);

    AiSkillVO updateSkill(Long skillId, AiSkillSaveRequest request);

    AiSkillVO publish(Long skillId);

    AiSkillVO offline(Long skillId);

    void deleteSkill(Long skillId);

    void collect(Long skillId);

    void cancelCollect(Long skillId);

    PageResult<AiSkillVO> pageCollections(Integer pageNum, Integer pageSize);

    AiSkillVO importZip(MultipartFile file, String skillName, String description, String skillType,
                        String subjectType, String targetAudience, String visibility, List<Long> categoryIds);

    ResponseEntity<byte[]> exportSkill(Long skillId);

    AiSkillInvokeVO invokeSkill(Long skillId, AiSkillInvokeRequest request);

    List<AiSkillCategoryVO> listCategories(String categoryType, Boolean enabledOnly);

    AiSkillCategoryVO createCategory(AiSkillCategorySaveRequest request);

    AiSkillCategoryVO updateCategory(Long categoryId, AiSkillCategorySaveRequest request);

    void deleteCategory(Long categoryId);
}

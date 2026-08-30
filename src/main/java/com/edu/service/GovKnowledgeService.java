package com.edu.service;

import com.edu.pojo.dto.gov.GovKnowledgeNodeCreateRequest;
import com.edu.pojo.dto.gov.GovKnowledgeNodeUpdateRequest;
import com.edu.pojo.dto.gov.GovKnowledgeProgressUpdateRequest;
import com.edu.pojo.dto.gov.GovKnowledgeNoteSaveRequest;
import com.edu.pojo.dto.gov.GovKnowledgeAnnotationSaveRequest;
import com.edu.pojo.dto.gov.GovKnowledgeCompareSaveRequest;
import com.edu.common.PageResult;
import com.edu.pojo.vo.gov.GovKnowledgeNodeVO;
import com.edu.pojo.vo.gov.GovKnowledgeCompareVO;
import com.edu.pojo.vo.gov.GovKnowledgeFavoriteVO;
import com.edu.pojo.vo.gov.GovKnowledgeFavoriteItemVO;
import com.edu.pojo.vo.gov.GovKnowledgeAnnotationVO;
import com.edu.pojo.vo.gov.GovKnowledgeNoteItemVO;
import com.edu.pojo.vo.gov.GovKnowledgeProgressVO;
import com.edu.pojo.vo.gov.GovKnowledgeNoteVO;

import java.util.List;

public interface GovKnowledgeService {
    List<GovKnowledgeNodeVO> listKnowledgeTree(String subject, String keyword);

    List<GovKnowledgeNodeVO> listAdminKnowledgeTree(String subject, String keyword);

    GovKnowledgeNodeVO getKnowledgeNode(Long nodeId);

    GovKnowledgeNodeVO getAdminKnowledgeNode(Long nodeId);

    GovKnowledgeProgressVO getKnowledgeProgress(Long knowledgeId);

    GovKnowledgeProgressVO updateKnowledgeProgress(Long knowledgeId, GovKnowledgeProgressUpdateRequest request);

    List<GovKnowledgeCompareVO> listKnowledgeCompare(Long knowledgeId);

    List<GovKnowledgeCompareVO> listAdminKnowledgeCompare(Long knowledgeId);

    GovKnowledgeCompareVO saveKnowledgeCompare(Long knowledgeId, GovKnowledgeCompareSaveRequest request);

    GovKnowledgeCompareVO updateKnowledgeCompare(Long compareId, GovKnowledgeCompareSaveRequest request);

    void deleteKnowledgeCompare(Long compareId);

    GovKnowledgeFavoriteVO getKnowledgeFavorite(Long knowledgeId);

    GovKnowledgeFavoriteVO collectKnowledge(Long knowledgeId);

    GovKnowledgeFavoriteVO cancelKnowledgeFavorite(Long knowledgeId);

    GovKnowledgeNoteVO getKnowledgeNote(Long knowledgeId);

    GovKnowledgeNoteVO saveKnowledgeNote(Long knowledgeId, GovKnowledgeNoteSaveRequest request);

    List<GovKnowledgeAnnotationVO> listKnowledgeAnnotations(Long knowledgeId);

    GovKnowledgeAnnotationVO saveKnowledgeAnnotation(Long knowledgeId, GovKnowledgeAnnotationSaveRequest request);

    GovKnowledgeAnnotationVO updateKnowledgeAnnotation(Long annotationId, GovKnowledgeAnnotationSaveRequest request);

    void deleteKnowledgeAnnotation(Long annotationId);

    PageResult<GovKnowledgeFavoriteItemVO> pageMyFavoriteKnowledge(Integer pageNum, Integer pageSize, String keyword);

    PageResult<GovKnowledgeNoteItemVO> pageMyKnowledgeNotes(Integer pageNum, Integer pageSize, String keyword);

    PageResult<GovKnowledgeAnnotationVO> pageMyKnowledgeAnnotations(Integer pageNum, Integer pageSize, String keyword);

    GovKnowledgeNodeVO createKnowledgeNode(GovKnowledgeNodeCreateRequest request);

    GovKnowledgeNodeVO updateKnowledgeNode(Long nodeId, GovKnowledgeNodeUpdateRequest request);

    void deleteKnowledgeNode(Long nodeId);
}

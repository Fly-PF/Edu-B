package com.edu.service;

import com.edu.pojo.dto.gov.GovMaterialSaveRequest;
import com.edu.common.PageResult;
import com.edu.pojo.vo.gov.GovMaterialVO;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;

public interface GovMaterialService {
    List<GovMaterialVO> listPublishedMaterials(Long categoryId);

    PageResult<GovMaterialVO> listMaterialsForAdmin(Long categoryId, Integer status, Integer pageNum, Integer pageSize);

    GovMaterialVO createMaterial(GovMaterialSaveRequest request, MultipartFile file);

    GovMaterialVO updateMaterial(Long materialId, GovMaterialSaveRequest request, MultipartFile file);

    ResponseEntity<byte[]> readPublishedFile(String fileUrl);

    void publishMaterial(Long materialId);

    void withdrawMaterial(Long materialId);

    void deleteMaterial(Long materialId);
}

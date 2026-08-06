package com.edu.service.safety;

import com.edu.common.PageResult;
import com.edu.pojo.dto.safety.SafetyDashboardDTO;
import com.edu.pojo.dto.safety.SafetyGatewayRequest;
import com.edu.pojo.dto.safety.SafetyGatewayResponse;
import com.edu.pojo.dto.safety.SafetyReviewActionRequest;
import com.edu.pojo.dto.safety.SafetyRecordDTO;
import com.edu.pojo.enums.safety.SafetyDecision;
import com.edu.pojo.enums.safety.SafetyGradeLevel;
import com.edu.pojo.enums.safety.SafetyRiskLevel;
import com.edu.pojo.enums.safety.SafetyRiskType;
import com.edu.pojo.enums.safety.SafetyReviewStatus;
import com.edu.pojo.enums.safety.SafetyScene;
import com.edu.pojo.enums.safety.SafetySourceModule;
import com.edu.pojo.enums.safety.SafetyUserRole;

public interface SafetyRecordService {
    Long recordEvaluation(SafetyGatewayRequest request, SafetyGatewayResponse response);

    PageResult<SafetyRecordDTO> pageRecords(Integer pageNum,
                                            Integer pageSize,
                                            SafetySourceModule sourceModule,
                                            SafetyScene scene,
                                            SafetyUserRole userRole,
                                            SafetyGradeLevel gradeLevel,
                                            SafetyRiskLevel riskLevel,
                                            SafetyRiskType riskType,
                                            SafetyDecision decision,
                                            SafetyReviewStatus reviewStatus,
                                            Boolean manualReviewRequired,
                                            String keyword);

    PageResult<SafetyRecordDTO> pageReviewRecords(Integer pageNum,
                                                  Integer pageSize,
                                                  Long classId,
                                                  SafetySourceModule sourceModule,
                                                  SafetyScene scene,
                                                  SafetyUserRole userRole,
                                                  SafetyGradeLevel gradeLevel,
                                                  SafetyRiskLevel riskLevel,
                                                  SafetyRiskType riskType,
                                                  SafetyDecision decision,
                                                  SafetyReviewStatus reviewStatus,
                                                  Boolean manualReviewRequired,
                                                  String keyword);

    SafetyRecordDTO detail(Long id);

    SafetyRecordDTO reviewDetail(Long id);

    SafetyRecordDTO approveReview(Long id, SafetyReviewActionRequest request);

    SafetyRecordDTO rejectReview(Long id, SafetyReviewActionRequest request);

    SafetyDashboardDTO dashboard(Integer days);
}

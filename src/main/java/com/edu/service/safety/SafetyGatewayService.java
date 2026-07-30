package com.edu.service.safety;

import com.edu.pojo.dto.safety.SafetyGatewayRequest;
import com.edu.pojo.dto.safety.SafetyGatewayResponse;

public interface SafetyGatewayService {
    SafetyGatewayResponse evaluate(SafetyGatewayRequest request);
}

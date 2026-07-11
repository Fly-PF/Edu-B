package com.edu.service;

import com.edu.pojo.dto.UpdateUserProfileRequest;
import com.edu.pojo.vo.UserProfileVO;

public interface UserProfileService {
    UserProfileVO getProfile();

    void updateProfile(UpdateUserProfileRequest request);
}

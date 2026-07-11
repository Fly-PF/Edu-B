package com.edu.service;

import org.springframework.web.multipart.MultipartFile;

public interface UserAvatarService {
    String uploadAvatar(MultipartFile file);

    String uploadAvatar(Long userId, MultipartFile file);

    String getAvatar();

}

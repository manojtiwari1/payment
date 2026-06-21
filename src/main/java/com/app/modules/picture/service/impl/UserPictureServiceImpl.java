package com.app.modules.picture.service.impl;

import com.app.common.constants.AppConstants;
import com.app.common.enums.ResponseCode;
import com.app.common.exception.ApplicationException;
import com.app.infrastructure.security.service.AuthService;
import com.app.modules.picture.service.UserPictureService;
import com.app.modules.user.entity.User;
import com.app.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPictureServiceImpl implements UserPictureService {

    @Value("${file.upload.max-size}")
    private Long MAX_FILE_SIZE;

//    private final AwsS3Service awsS3Service;

    private final AuthService authService;

    private final UserRepository userRepository;

//    @Override
//    @Transactional
//    public UserResponseDTO uploadProfilePicture(MultipartFile file) {
//
//        User currentUser = authService.getCurrentUser();
//
//        validateFile(file);
//
//        // Generate S3 key
//        String key = generateKey(currentUser.getId(), file.getOriginalFilename());
//
//        try {
//            awsS3Service.upload(file.getInputStream(), file.getSize(), key);
//        } catch (Exception e) {
//            log.error("Failed to upload profile picture", e);
//            throw new ApplicationException(
//                    ResponseCode.FAILED,
//                    "Failed to upload profile picture"
//            );
//        }
//
//        // Delete old picture from S3 (if exists)
//        if (currentUser.getProfilePictureUrl() != null) {
//            try {
//                awsS3Service.delete(extractKeyFromUrl(currentUser.getProfilePictureUrl()));
//            } catch (Exception e) {
//                log.warn("Old profile picture deletion failed", e);
//            }
//        }
//
//        String imageUrl = awsS3Service.getPublicUrl(key).toString();
//
//        // Save picture entity
//        Picture picture = new Picture();
//        picture.setType(PictureType.PROFILE_PICTURE);
//        picture.setUrl(imageUrl);
//
//        currentUser.setProfilePictureUrl(imageUrl);
//        currentUser.setPicture(picture);
//
//        userRepository.save(currentUser);
//
//        return UserResponseDTO.from(currentUser);
//    }

    @Override
    @Transactional
    public void deleteProfilePicture() {

        User currentUser = authService.getCurrentUser();

//        if (currentUser.getProfilePictureUrl() == null) {
//            return;
//        }
//
//        try {
//            awsS3Service.delete(
//                    extractKeyFromUrl(currentUser.getProfilePictureUrl())
//            );
//        } catch (Exception e) {
//            log.warn("Failed to delete profile picture from S3", e);
//        }

        currentUser.setProfilePictureUrl(null);
        currentUser.setPicture(null);

        userRepository.save(currentUser);
    }

    /* =========================
       VALIDATION
       ========================= */

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ApplicationException(
                    ResponseCode.BAD_REQUEST,
                    "File cannot be empty"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApplicationException(
                    ResponseCode.BAD_REQUEST,
                    "File size exceeds 5MB limit"
            );
        }

        if (!AppConstants.ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ApplicationException(
                    ResponseCode.BAD_REQUEST,
                    "Unsupported file type"
            );
        }
    }

    private String generateKey(Long userId, String originalFilename) {
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";

        return "users/" + userId + "/profile/" +
                UUID.randomUUID() + extension;
    }

    private String extractKeyFromUrl(String url) {
        return url.substring(url.indexOf(".com/") + 5);
    }
}

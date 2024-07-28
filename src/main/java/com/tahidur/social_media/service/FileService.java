package com.tahidur.social_media.service;


import org.springframework.web.multipart.MultipartFile;

public interface FileService {
     String uploadImage(String path, MultipartFile file);
}

package com.tahidur.social_media.controller;

import com.tahidur.social_media.model.AppUser;
import com.tahidur.social_media.repository.UserRepository;
import com.tahidur.social_media.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@RestController
public class UserController {
    public static String UPLOAD_DIRECTORY = System.getProperty("user.dir") + "/uploads";

    @Autowired
    UserService userService;
    @Autowired
    private UserRepository userRepository;

    @GetMapping("users")
    public List<AppUser> Users() {
        return userRepository.findAll();
    }

    @GetMapping("user/{id}")
    public Optional<AppUser> UserDetails(Long id) {
        return userService.getUser(id);
    }

    @PostMapping(value = "create-user", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> CreateUser(@RequestParam("username") String username, @RequestParam("image") MultipartFile image) throws IOException {
        StringBuilder imageName = new StringBuilder();
        Path imagePath = Paths.get(UPLOAD_DIRECTORY, image.getOriginalFilename());
        imageName.append(image.getOriginalFilename());
        Files.write(imagePath, image.getBytes());
        AppUser user = new AppUser(username, imageName.toString());
        return userService.createUser(user);
    }

    @GetMapping("/admin/home")
    public String AdminHome() {
        return "Admin Panel";
    }

    @GetMapping("/user/home")
    public String UserHome() {
        return "User Panel";
    }
}

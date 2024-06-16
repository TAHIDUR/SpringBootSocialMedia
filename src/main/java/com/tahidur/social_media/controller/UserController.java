package com.tahidur.social_media.controller;

import com.tahidur.social_media.model.User;
import com.tahidur.social_media.repository.UserRepository;
import com.tahidur.social_media.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class UserController {
    @Autowired
    UserService userService;
    @Autowired
    private UserRepository userRepository;

    @GetMapping("users")
    public List<User> Users(){
        return userRepository.findAll();
    }

    @GetMapping("user/{id}")
    public Optional<User> UserDetails(Long id){
        return userService.getUser(id);
    }

    @PostMapping("create-user")
    public ResponseEntity<String> CreateUser(@RequestBody Map<String, String> body){
        String username = body.get("username");
        return userService.createUser(new User(username));
    }
}

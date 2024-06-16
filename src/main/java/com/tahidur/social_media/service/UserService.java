package com.tahidur.social_media.service;

import com.tahidur.social_media.model.User;
import com.tahidur.social_media.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public Optional<User> getUser(Long id){
        if(userRepository.existsById(id)) return userRepository.findById(id);
        return Optional.empty();
    }

    public ResponseEntity<String> createUser(User user){
        try {
            userRepository.save(user);
            return new ResponseEntity<>("User created Successfully", HttpStatus.OK);
        } catch (DataIntegrityViolationException e){
            if(e.getCause() instanceof ConstraintViolationException){
                return new ResponseEntity<>("Username already taken", HttpStatus.CONFLICT);
            }
            return new ResponseEntity<>("Data Integrity violation", HttpStatus.BAD_REQUEST);
        } catch (Exception e){
            return new ResponseEntity<>("An error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

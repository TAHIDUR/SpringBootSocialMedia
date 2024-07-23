package com.tahidur.social_media.service;

import com.tahidur.social_media.model.AppUser;
import com.tahidur.social_media.model.Users;
import com.tahidur.social_media.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import java.sql.*;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public Optional<AppUser> getUser(Long id){
        if(userRepository.existsById(id)) return userRepository.findById(id);
        return Optional.empty();
    }

    public ResponseEntity<String> createUser(AppUser user){
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

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<AppUser> user = userRepository.findByUsername(username);
        return user.map(appUser -> User.withUsername(appUser.getUsername())
                .password(appUser.getPassword())
                .roles(String.valueOf(appUser.getRole()))
                .build()).orElse(null);
    }

    public int updateStatus(String status,
                            AppUser user,
                            Boolean loggedOut){
        if(status.equals("offline")){
            user.setIsOnline(false);
            if(loggedOut) user.setLastLoggedIn(Timestamp.from(Instant.now()));
            userRepository.save(user);
            return 200;
        } else if (status.equals("online")) {
            user.setIsOnline(true);
            userRepository.save(user);
            return 200;
        }
        return 400;
    }

    public List<Users> userStatuses() {
        List<Users> users = new ArrayList<>();
        try(Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/social-media", "root", "");
            Statement statement = connection.createStatement()
        ) {
            ResultSet result = statement.executeQuery("SELECT username, is_online FROM users");
            while (result.next()){
                users.add(new Users(result.getString(1), result.getBoolean(2)));
            }
        }catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return users;
    }
}

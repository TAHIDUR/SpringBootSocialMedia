package com.tahidur.social_media.controller;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.tahidur.social_media.model.*;
import com.tahidur.social_media.repository.UserRepository;
import com.tahidur.social_media.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@RestController
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Value("${security.jwt.secret-key}")
    private String jwtSecretKey;


    @Value("${security.jwt.issuer}")
    private String jwtIssuer;

    @GetMapping("users")
    public List<AppUser> Users() {
        return userRepository.findAll();
    }

    @GetMapping("user/{id}")
    public Optional<AppUser> UserDetails(Long id) {
        return userService.getUser(id);
    }

    @PostMapping("register")
    public ResponseEntity<Object> Register(
            @Valid @RequestBody RegisterDto registerDto, BindingResult result) {
        if (result.hasErrors()) {
            var errorList = result.getAllErrors();
            var errorMap = new HashMap<String, String>();

            errorList.forEach(error -> {
                String fieldName = ((org.springframework.validation.FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                errorMap.put(fieldName, errorMessage);
            });

            return ResponseEntity.badRequest().body(errorMap.toString());
        }

        var bCryptEncoder = new BCryptPasswordEncoder();

        AppUser user = new AppUser();
        user.setUsername(registerDto.getUsername());
        user.setEmail(registerDto.getEmail());
        user.setPhone(registerDto.getPhone());
        user.setRole(Role.USER);
        user.setPassword(bCryptEncoder.encode(registerDto.getPassword()));

        try {

            var userExist = userRepository.findByUsername(registerDto.getUsername());
            if (userExist.isPresent()) {
                return ResponseEntity.badRequest().body("Username already taken");
            }

            var emailExist = userRepository.findByEmail(registerDto.getEmail());
            if (emailExist.isPresent()) {
                return ResponseEntity.badRequest().body("Email already taken");
            }

            user.setIsOnline(true);
            userRepository.save(user);

            var jwtToken = createJwtToken(user);

            var response = new HashMap<String, Object>();
            response.put("token", jwtToken);
            response.put("user", user);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("Something went wrong");
            e.printStackTrace();
        }

        return ResponseEntity.badRequest().body("Something went wrong");
    }

    @PostMapping("login")
    public ResponseEntity<Object> Login(
            @Valid @RequestBody LoginDto loginDto,
            BindingResult result
    ) {
        if (result.hasErrors()) {
            var errorList = result.getAllErrors();
            var errorMap = new HashMap<String, String>();

            errorList.forEach(error -> {
                String fieldName = ((org.springframework.validation.FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                errorMap.put(fieldName, errorMessage);
            });

            return ResponseEntity.badRequest().body(errorMap.toString());
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDto.getUsername(),
                            loginDto.getPassword()
                    )
            );

            Optional<AppUser> user = userRepository.findByUsername(loginDto.getUsername());

            if (user.isPresent()) {
                AppUser currentUser = user.get();
                String jwtToken = createJwtToken(currentUser);

                currentUser.setIsOnline(true);
                userRepository.save(currentUser);

                var response = new HashMap<String, Object>();
                response.put("token", jwtToken);
                response.put("user", currentUser);

                return ResponseEntity.ok(response);
            }

            return ResponseEntity.badRequest().body("Username or password is wrong");
        } catch (Exception e) {
            System.out.println("An error occurred");
            e.printStackTrace();
        }

        return ResponseEntity.badRequest().body("Something went wrong");
    }

    @GetMapping("/profile")
    public ResponseEntity<Object> Profile(Authentication auth) {
        var response = new HashMap<String, Object>();
        response.put("Username", auth.getName());
        response.put("Role", auth.getAuthorities());

        Optional<AppUser> user = userRepository.findByUsername(auth.getName());

        if (user.isPresent()) {
            AppUser currentUser = user.get();
            currentUser.setIsOnline(true);
            userRepository.save(currentUser);
            response.put("User", currentUser);
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.badRequest().body("User not found");
    }

    @PostMapping("change-status")
    public ResponseEntity<Object> changeStatus(@RequestBody UserStatus userStatus, Authentication auth) {
        Optional<AppUser> user = userRepository.findByUsername(auth.getName());
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }
        int status = userService.updateStatus(userStatus.getStatus(), user.get());
        if(status == 200 ) {
            return ResponseEntity.ok(userStatus);
        }else
            return ResponseEntity.badRequest().body("Something went wrong");
    }

    private String createJwtToken(AppUser user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtIssuer)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(24 * 3600))
                .subject(user.getUsername())
                .claim("role", user.getRole())
                .build();

        var encoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(jwtSecretKey.getBytes())
        );

        var params = JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims
        );

        return encoder.encode(params).getTokenValue();
    }

    @GetMapping("/admin/home")
    public String AdminHome() {
        return "Admin Panel";
    }

    @GetMapping("/user/home")
    public String UserHome() {
        return "User Panel";
    }

    @GetMapping("/logout")
    public ResponseEntity<Object> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        return ResponseEntity.ok("Logged out");
    }
}

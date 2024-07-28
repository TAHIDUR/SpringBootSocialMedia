package com.tahidur.social_media.controller;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.tahidur.social_media.model.*;
import com.tahidur.social_media.repository.UserRepository;
import com.tahidur.social_media.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@RestController
public class UserController {
    private final HashMap<String, Object> response = new HashMap<>();
    public static String UPLOAD_DIRECTORY = System.getProperty("user.dir") + "/uploads";

    @Autowired
    private UserService userService;

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

    @GetMapping("userStatuses")
    public ResponseEntity<Object> AllUsers() {
        try{
            List<Users> users = userService.userStatuses();
            response.put("users", users);
        }catch (Exception e) {
            e.printStackTrace();
            response.put("Error", "Error occurred: "+e.getMessage());
        }
        return ResponseEntity.ok(response);
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
        user.setDesignation(registerDto.getDesignation());
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
            user.setLastLoggedIn(Timestamp.from(Instant.now()));
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
                currentUser.setLastLoggedIn(Timestamp.from(Instant.now()));
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
        int status = userService.updateStatus(userStatus.getStatus(), user.get(), userStatus.getLoggedOut());
        if (status == 200) {
            try{
                List<Users> users = userService.userStatuses();
                response.put("users", users);
            }catch (Exception e) {
                response.put("Error", "Error occurred: "+e.getMessage());
            }
            return ResponseEntity.ok(response);
        } else
            return ResponseEntity.badRequest().body("Something went wrong");
    }

    @PostMapping(value = "update-profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> updateProfile(@RequestParam("username") String name,
                                                @RequestParam("email") String email,
                                                @RequestParam("designation") String designation,
                                                @RequestParam("phone") String phone,
                                                @RequestParam("image") MultipartFile image,
                                                Authentication auth
    ) throws IOException {
        Optional<AppUser> userExist = userRepository.findByUsername(auth.getName());
        if(userExist.isPresent()){
            AppUser currentUser = userExist.get();
            currentUser.setUsername(name);
            currentUser.setEmail(email);
            currentUser.setDesignation(designation);
            currentUser.setPhone(phone);
            StringBuilder imageName = new StringBuilder();
            Path imagePath = Paths.get(UPLOAD_DIRECTORY, image.getOriginalFilename());
            imageName.append(image.getOriginalFilename());
            Files.write(imagePath, image.getBytes());
            currentUser.setImage(imageName.toString());
            userRepository.save(currentUser);
            response.put("success", "User Updated");
            return ResponseEntity.ok(response);
        }
        response.put("Error", "Something went wrong");
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping(value = "profile-image", produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] profileImage(Authentication auth) throws IOException{

        Optional<AppUser> user = userRepository.findByUsername(auth.getName());
        if(user.isPresent()){
            String imageFile = user.get().getImage();
            Path imagePath = Paths.get(UPLOAD_DIRECTORY, imageFile);
            return Files.readAllBytes(new File(String.valueOf(imagePath)).toPath());
        }
        Path imagePath = Paths.get(UPLOAD_DIRECTORY, "images.jpg");
        return Files.readAllBytes(new File(String.valueOf(imagePath)).toPath());
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
}

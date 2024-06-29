package com.tahidur.social_media.model;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDto {

    @NotEmpty
    private String username;

    @NotEmpty
    private String email;

    private String phone;

    @NotEmpty
    @Size(min = 6, message = "Minimum 6 character required")
    private String password;
}

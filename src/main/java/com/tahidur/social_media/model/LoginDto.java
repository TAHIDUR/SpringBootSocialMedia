package com.tahidur.social_media.model;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class LoginDto {

    @NotEmpty
    private String username;

    @NotEmpty
    private String password;
}

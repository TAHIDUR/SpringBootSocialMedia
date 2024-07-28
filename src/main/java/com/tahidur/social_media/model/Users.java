package com.tahidur.social_media.model;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class Users {
    private String username;
    private Boolean isOnline;
}

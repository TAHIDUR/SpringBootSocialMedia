package com.tahidur.social_media.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Users {
    private String name;
    private Boolean isOnline;
}

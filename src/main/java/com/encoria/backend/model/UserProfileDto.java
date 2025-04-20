package com.encoria.backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDate birthdate;
    private String pictureUrl;

}

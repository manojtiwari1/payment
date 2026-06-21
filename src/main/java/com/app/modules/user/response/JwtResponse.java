package com.app.modules.user.response;

import lombok.*;

@NoArgsConstructor
@Getter
@Setter
public class JwtResponse {

    private String firstName;

    private String lastName;

    private String fullName;

    private String contactNo;

    private String token;

    private String refreshToken;

    private Long id;

    private String email;

    private String userName;

    private Long expireIn;

    private Long refreshExpireIn;


    public JwtResponse(String firstName, String lastName, String contactNo,
                       String token, String refreshToken,Long expireIn, Long refreshExpireIn, Long id, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = firstName + " " + lastName;
        this.contactNo = contactNo;
        this.token = token;
        this.refreshToken = refreshToken;
        this.id = id;
        this.email = email;
        this.userName = email;
        this.expireIn = expireIn;
        this.refreshExpireIn = refreshExpireIn;
    }
}

package com.lakshya.cycletracker.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SignupForm {

    @NotBlank(message = "Please enter your name.")
    @Size(max = 60)
    private String displayName;

    @NotBlank(message = "Please enter your email.")
    @Email(message = "That doesn't look like an email address.")
    @Size(max = 254)
    private String email;

    // BCrypt only uses the first 72 bytes of a password.
    @Size(min = 8, max = 72, message = "Use 8 to 72 characters.")
    private String password;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

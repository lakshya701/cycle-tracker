package com.lakshya.cycletracker.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

// Table is "app_user" because "user" is a reserved word in PostgreSQL.
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 60)
    private String displayName;

    @Column(nullable = false)
    private boolean remindersEnabled = true;

    @Column(nullable = false)
    private LocalDate createdOn = LocalDate.now();

    protected AppUser() {}

    public AppUser(String email, String passwordHash, String displayName) {
        this.email = email.trim().toLowerCase();
        this.passwordHash = passwordHash;
        this.displayName = displayName.trim();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName.trim(); }
    public boolean isRemindersEnabled() { return remindersEnabled; }
    public void setRemindersEnabled(boolean remindersEnabled) { this.remindersEnabled = remindersEnabled; }
    public LocalDate getCreatedOn() { return createdOn; }
}

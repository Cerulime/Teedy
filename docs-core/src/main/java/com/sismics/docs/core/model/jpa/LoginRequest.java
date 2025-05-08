package com.sismics.docs.core.model.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "T_LOGIN_REQUEST")
public class LoginRequest {
    @Id
    @Column(name = "LR_ID_C", length = 36)
    private final String id;
    
    @Column(name = "LR_TOKEN_C", nullable = false, length = 100)
    private String token;
    
    @Column(name = "LR_IP_C", nullable = false, length = 45)
    private String ip;
    
    @Column(name = "LR_TIMESTAMP_D", nullable = false)
    private final Date timestamp;
    
    @Column(name = "LR_STATUS_C", nullable = false, length = 20)
    private String status;

    public LoginRequest() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = new Date();
        this.status = "PENDING";
    }

    public LoginRequest(String token, String ip) {
        this();
        this.token = Objects.requireNonNull(token, "Token cannot be null");
        this.ip = Objects.requireNonNull(ip, "IP cannot be null");
    }

    public String getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public String getIp() {
        return ip;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getStatus() {
        return status;
    }

    public LoginRequest setToken(String token) {
        this.token = Objects.requireNonNull(token, "Token cannot be null");
        return this;
    }

    public LoginRequest setIp(String ip) {
        this.ip = Objects.requireNonNull(ip, "IP cannot be null");
        return this;
    }

    public LoginRequest setStatus(String status) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoginRequest that = (LoginRequest) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

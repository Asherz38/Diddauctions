package com.example.identity;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AuthController {
  private final AuthService auth;
  private final UserRepository repo;
  
  public AuthController(AuthService auth, UserRepository repo){ 
    this.auth = auth; this.repo = repo; 
  }

  @PostMapping("/auth/signup")
  public ResponseEntity<?> signup(@RequestBody Map<String,String> body){
    try{
      User u = auth.register(body.get("username"), body.get("password"),
        body.getOrDefault("firstName",""), body.getOrDefault("lastName",""),
        body.getOrDefault("streetName",""), body.getOrDefault("streetNumber",""),
        body.getOrDefault("city",""), body.getOrDefault("country",""), body.getOrDefault("postalCode",""));

      return ResponseEntity.ok(Map.of("id", u.id, "username", u.username));

    }catch (IllegalArgumentException ex){
      
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
  }

  @PostMapping("/auth/login")
  public ResponseEntity<?> login(@RequestBody Map<String,String> body){
    return auth.authenticate(body.get("username"), body.get("password"))
      .<ResponseEntity<?>>map(u -> ResponseEntity.ok(Map.of("userId", u.id, "username", u.username)))
      .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error","invalid credentials")));
  }

  @GetMapping("/users/{id}")
  public ResponseEntity<?> getUser(@PathVariable int id){
    return repo.findById(id)
      .<ResponseEntity<?>>map(u -> ResponseEntity.ok(Map.of(
        "id", u.id,
        "username", u.username,
        "firstName", u.firstName,
        "lastName", u.lastName,
        "streetName", u.streetName,
        "streetNumber", u.streetNumber,
        "city", u.city,
        "country", u.country,
        "postalCode", u.postalCode)))
      .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/users/by-username/{username}")
  public ResponseEntity<?> getByUsername(@PathVariable String username){
    return repo.findByUsername(username.toLowerCase())
      .<ResponseEntity<?>>map(u -> ResponseEntity.ok(Map.of(
        "id", u.id,
        "username", u.username,
        "firstName", u.firstName,
        "lastName", u.lastName,
        "streetName", u.streetName,
        "streetNumber", u.streetNumber,
        "city", u.city,
        "country", u.country,
        "postalCode", u.postalCode)))
      .orElseGet(() -> ResponseEntity.notFound().build());
  }
}


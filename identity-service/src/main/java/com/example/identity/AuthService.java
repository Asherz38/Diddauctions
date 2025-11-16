package com.example.identity;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
  private final UserRepository repo;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  public AuthService(UserRepository repo){ 
    this.repo = repo; 
  }

  public User register(String username, String rawPassword, String first, String last,
  String streetName, String streetNumber, String city, String country, String postal){

    if (repo.findByUsername(username).isPresent()) throw new IllegalArgumentException("username exists");

    User u = new User();

    u.username = username.toLowerCase();

    u.passwordHash = encoder.encode(rawPassword);

    u.firstName = first; u.lastName = last;

    u.streetName = streetName; u.streetNumber = streetNumber; u.city = city; u.country = country; u.postalCode = postal;

    repo.create(u);

    return repo.findByUsername(u.username).orElseThrow();
    
  }

  public Optional<User> authenticate(String username, String rawPassword){
    return repo.findByUsername(username.toLowerCase())
      .filter(u -> encoder.matches(rawPassword, u.passwordHash));
  }
}



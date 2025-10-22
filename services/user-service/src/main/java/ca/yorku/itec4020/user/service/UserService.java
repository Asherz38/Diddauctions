package ca.yorku.itec4020.user.service;

import ca.yorku.itec4020.user.entity.UserEntity;
import ca.yorku.itec4020.user.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {
  private final UserRepository repo;

  public UserService(UserRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public UserEntity signUp(UserEntity req) {
    // naive: no hashing, minimal checks; expand as needed
    return repo.save(req);
  }

  @Transactional(readOnly = true)
  public Optional<UserEntity> signIn(String username, String password) {
    return repo.findByUsername(username)
        .filter(u -> u.getPassword().equals(password));
  }

  @Transactional(readOnly = true)
  public Optional<UserEntity> getByUsername(String username) {
    return repo.findByUsername(username);
  }

  @Transactional(readOnly = true)
  public Optional<UserEntity> getById(long id) {
    return repo.findById(id);
  }

  @Transactional
  public UserEntity save(UserEntity user) {
    return repo.save(user);
  }
}

package ca.yorku.itec4020.controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"ca.yorku.itec4020"})
public class ControllerAppApplication {
  public static void main(String[] args) {
    SpringApplication.run(ControllerAppApplication.class, args);
  }
}

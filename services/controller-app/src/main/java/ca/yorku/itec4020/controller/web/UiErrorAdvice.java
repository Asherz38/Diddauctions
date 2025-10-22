package ca.yorku.itec4020.controller.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UiErrorAdvice {

  @ExceptionHandler(Exception.class)
  public Object handleUiErrors(Exception ex, HttpServletRequest req, Model model) throws Exception {
    // If the request is for the UI (path starts with /ui) or the client accepts HTML,
    // render a friendly HTML error page instead of JSON.
    String uri = req.getRequestURI();
    String accept = req.getHeader("Accept");
    boolean isUiPath = uri != null && uri.startsWith("/ui");
    boolean wantsHtml = accept != null && accept.contains("text/html");
    if (isUiPath || wantsHtml) {
      String message = ex.getMessage();
      if (message == null || message.isBlank()) message = "Unexpected error";
      model.addAttribute("message", message);
      return "error";
    }
    // For non-HTML requests, let other handlers (e.g., REST JSON advice) process it
    throw ex;
  }
}

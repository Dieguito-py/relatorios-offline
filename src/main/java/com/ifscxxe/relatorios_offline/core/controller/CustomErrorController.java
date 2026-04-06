package com.ifscxxe.relatorios_offline.core.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.boot.web.servlet.error.ErrorController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.time.LocalDateTime;

@RequestMapping("/error") // Maps all error requests to this controller
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping
    public Object handleError(HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.resolve(response.getStatus());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String message = extractMessage(request, status);
        if (isApiRequest(request)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", status.value());
            body.put("error", status.getReasonPhrase());
            body.put("message", message);
            body.put("path", getOriginalPath(request));
            body.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.status(status).body(body);
        }

        ModelAndView mav = new ModelAndView("error/404");
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("error", status.getReasonPhrase());
        mav.addObject("message", message);
        mav.addObject("timestamp", LocalDateTime.now());
        return mav;
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String originalPath = getOriginalPath(request);
        return originalPath != null && originalPath.startsWith("/api/");
    }

    private String getOriginalPath(HttpServletRequest request) {
        Object originalUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (originalUri instanceof String uri && !uri.isBlank()) {
            return uri;
        }
        return request.getRequestURI();
    }

    private String extractMessage(HttpServletRequest request, HttpStatus status) {
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        if (exception instanceof Throwable throwable && throwable.getMessage() != null) {
            return throwable.getMessage();
        }
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        if (message != null) {
            return message.toString();
        }
        if (status == HttpStatus.BAD_REQUEST) {
            return "A requisição contém dados inválidos.";
        }
        if (status == HttpStatus.NOT_IMPLEMENTED) {
            return "Funcionalidade ainda não disponível.";
        }
        return "O endereço acessado não existe ou foi movido.";
    }
}

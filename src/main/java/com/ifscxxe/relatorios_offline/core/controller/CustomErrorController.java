package com.ifscxxe.relatorios_offline.core.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.boot.web.servlet.error.ErrorController;

import java.time.LocalDateTime;

@RequestMapping("/error") // Maps all error requests to this controller
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping
    public ModelAndView handleError(HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.resolve(response.getStatus());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        ModelAndView mav = new ModelAndView("error/404");
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("error", status.getReasonPhrase());
        mav.addObject("message", extractMessage(request, status));
        mav.addObject("timestamp", LocalDateTime.now());
        return mav;
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
        // Fallback messages for specific HTTP status codes
        if (status == HttpStatus.BAD_REQUEST) {
            return "A requisição contém dados inválidos.";
        }
        if (status == HttpStatus.NOT_IMPLEMENTED) {
            return "Funcionalidade ainda não disponível.";
        }
        return "O endereço acessado não existe ou foi movido.";
    }
}

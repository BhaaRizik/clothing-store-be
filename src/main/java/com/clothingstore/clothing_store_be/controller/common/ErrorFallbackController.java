package com.clothingstore.clothing_store_be.controller.common;

import com.clothingstore.clothing_store_be.constants.ErrorMessages;
import com.clothingstore.clothing_store_be.constants.LogMessages;
import com.clothingstore.clothing_store_be.exception.common.ProblemDetailFactory;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@Hidden
@RestController
@RequiredArgsConstructor
public class ErrorFallbackController implements ErrorController {

    private final ProblemDetailFactory problemDetailFactory;

    @RequestMapping(ErrorMessages.ERROR_PATH_PLACEHOLDER)
    public ResponseEntity<ProblemDetail> renderErrorProblem(HttpServletRequest request) {
        HttpStatusCode statusCode = resolveErrorStatus(request);
        Optional.ofNullable(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION))
                .filter(Throwable.class::isInstance)
                .map(Throwable.class::cast)
                .filter(exception -> statusCode.is5xxServerError())
                .ifPresent(exception -> log.error(LogMessages.ERROR_FALLBACK_SERVER_ERROR, exception));
        return ResponseEntity.status(statusCode)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemDetailFactory.createProblemForStatus(statusCode));
    }

    private HttpStatusCode resolveErrorStatus(HttpServletRequest request) {
        return Optional.ofNullable(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE))
                .filter(Integer.class::isInstance)
                .map(Integer.class::cast)
                .map(HttpStatus::resolve)
                .filter(HttpStatus::isError)
                .map(HttpStatusCode.class::cast)
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

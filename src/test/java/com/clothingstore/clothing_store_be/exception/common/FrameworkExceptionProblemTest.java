package com.clothingstore.clothing_store_be.exception.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FrameworkExceptionProblemTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FrameworkErrorTriggerEndpoints())
                .setControllerAdvice(new GlobalExceptionHandler(new ProblemDetailFactory()))
                .build();
    }

    private static ResultActions expectProblem(ResultActions result, int status, String code, String title,
            String detail) throws Exception {
        return result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.code").value(code));
    }

    @Test
    void missingRequestParameterReturns400ValidationErrorWithoutFrameworkText() throws Exception {
        expectProblem(mockMvc.perform(get("/framework-errors/required-parameter")), 400, "VALIDATION_ERROR",
                "Validation failed", "One or more request parameters are invalid.")
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isEmpty())
                .andExpect(content().string(not(containsString("secretParameter"))));
    }

    @Test
    void otherFramework4xxKeepsStatusAndUsesValidationErrorCode() throws Exception {
        expectProblem(mockMvc.perform(get("/framework-errors/upload-too-large")), 413, "VALIDATION_ERROR",
                "Validation failed", "One or more request parameters are invalid.")
                .andExpect(content().string(not(containsString("1024"))));
    }

    @Test
    void missingPathVariableReturns500InternalErrorWithoutFrameworkText() throws Exception {
        expectProblem(mockMvc.perform(get("/framework-errors/missing-path-variable")), 500, "INTERNAL_ERROR",
                "Internal server error", "An unexpected error occurred. Please try again later.")
                .andExpect(content().string(not(containsString("secretVariable"))));
    }

    @Test
    void responseWriteFailureReturns500InternalErrorWithoutFrameworkText() throws Exception {
        expectProblem(mockMvc.perform(get("/framework-errors/not-writable")), 500, "INTERNAL_ERROR",
                "Internal server error", "An unexpected error occurred. Please try again later.")
                .andExpect(content().string(not(containsString("secret converter failure"))))
                .andExpect(content().string(not(containsString("HttpMessageNotWritableException"))));
    }

    @Test
    void asyncRequestTimeoutReturns503ServiceUnavailable() throws Exception {
        expectProblem(mockMvc.perform(get("/framework-errors/async-timeout")), 503, "SERVICE_UNAVAILABLE",
                "Service unavailable", "The service is temporarily unavailable. Please try again later.");
    }
}

package com.clothingstore.clothing_store_be.exception.common;

import com.clothingstore.clothing_store_be.constants.LogMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.coyote.ActionCode;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
public class ProblemJsonErrorReportValve extends ErrorReportValve {

    private final ProblemDetailFactory problemDetailFactory;
    private final JsonMapper problemJsonMapper;

    @Override
    protected void report(Request request, Response response, Throwable throwable) {
        HttpStatusCode statusCode = HttpStatusCode.valueOf(response.getStatus());
        if (!statusCode.isError() || response.getContentWritten() > 0 || !response.setErrorReported()) {
            return;
        }
        AtomicBoolean ioAllowed = new AtomicBoolean(false);
        response.getCoyoteResponse().action(ActionCode.IS_IO_ALLOWED, ioAllowed);
        if (!ioAllowed.get()) {
            return;
        }
        try {
            String problemJson = problemJsonMapper.writeValueAsString(problemDetailFactory.createProblemForStatus(statusCode));
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            Optional<Writer> reporter = Optional.ofNullable(response.getReporter());
            if (reporter.isPresent()) {
                reporter.get().write(problemJson);
                response.finishResponse();
            }
        } catch (IOException | IllegalStateException | JacksonException exception) {
            log.warn(LogMessages.CONTAINER_ERROR_REPORT_FAILED, exception.getClass().getSimpleName());
        }
    }
}

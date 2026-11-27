package com.clothingstore.clothing_store_be.exception.common;

import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestController
@Profile("framework-error-triggers")
@RequestMapping("/framework-errors")
class FrameworkErrorTriggerEndpoints {

    @GetMapping("/required-parameter")
    String requireParameter(@RequestParam("secretParameter") String secretParameter) {
        return secretParameter;
    }

    @GetMapping("/upload-too-large")
    String rejectLargeUpload() {
        throw new MaxUploadSizeExceededException(1024);
    }

    @GetMapping("/missing-path-variable")
    String requireMissingPathVariable(@PathVariable("secretVariable") String secretVariable) {
        return secretVariable;
    }

    @GetMapping("/not-writable")
    String failWriting() {
        throw new HttpMessageNotWritableException("secret converter failure");
    }

    @GetMapping("/async-timeout")
    String timeOutAsyncRequest() {
        throw new AsyncRequestTimeoutException();
    }
}

package com.clothingstore.clothing_store_be.config;

import com.clothingstore.clothing_store_be.exception.common.ProblemDetailFactory;
import com.clothingstore.clothing_store_be.exception.common.ProblemJsonErrorReportValve;
import org.apache.catalina.Valve;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.valves.ErrorReportValve;
import org.springframework.boot.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.Optional;

@Configuration
public class TomcatErrorReportConfig {

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> problemJsonErrorReportCustomizer(
            ProblemDetailFactory problemDetailFactory, JsonMapper jsonMapper) {
        JsonMapper problemJsonMapper = jsonMapper.rebuild()
                .addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
                .build();
        return factory -> factory.addContextCustomizers(context -> Optional.ofNullable(context.getParent())
                .filter(StandardHost.class::isInstance)
                .map(StandardHost.class::cast)
                .ifPresent(host -> replaceErrorReportValves(host,
                        new ProblemJsonErrorReportValve(problemDetailFactory, problemJsonMapper))));
    }

    private static void replaceErrorReportValves(StandardHost host, Valve problemJsonValve) {
        Arrays.stream(host.getPipeline().getValves())
                .filter(ErrorReportValve.class::isInstance)
                .forEach(valve -> host.getPipeline().removeValve(valve));
        host.setErrorReportValveClass(ProblemJsonErrorReportValve.class.getName());
        host.getPipeline().addValve(problemJsonValve);
    }
}

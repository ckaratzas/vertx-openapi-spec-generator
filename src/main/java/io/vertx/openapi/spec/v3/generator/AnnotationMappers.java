package io.vertx.openapi.spec.v3.generator;

import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author ckaratza
 * Simple OpenApi Annotation mapping to OpenApi models. This part needs modifications to cover all spec.
 */
final class AnnotationMappers {

    private static final Logger log = LoggerFactory.getLogger(AnnotationMappers.class);

    static void decorateOperationFromAnnotation(Operation annotation, io.swagger.v3.oas.models.Operation operation) {
        operation.summary(annotation.summary());
        operation.description(annotation.description());
        operation.operationId(annotation.operationId());
        operation.deprecated(annotation.deprecated());

        ApiResponses apiResponses = new ApiResponses();
        apiResponses.putAll(
                Arrays.stream(annotation.responses()).map(response -> {
                    ApiResponse apiResponse = new ApiResponse();
                    apiResponse.description(response.description());
                    if (response.content().length > 0) {
                        Arrays.stream(response.content()).forEach(content -> {
                            Content c = getContent(content);
                            if (!Void.class.equals(content.array().schema().implementation()))
                                c.get(content.mediaType()).getSchema().setExample(clean(content.array().schema().example()));
                            else if (!Void.class.equals(content.schema().implementation()))
                                c.get(content.mediaType()).getSchema().setExample(content.schema().example());
                            apiResponse.content(c);
                        });
                    }
                    Arrays.stream(response.headers()).forEach(header -> {
                        Header h = new Header();
                        h.description(header.description());
                        h.deprecated(header.deprecated());
                        h.allowEmptyValue(header.allowEmptyValue());
                        Optional<Schema> schemaFromAnnotation = AnnotationsUtils.getSchemaFromAnnotation(header.schema());
                        schemaFromAnnotation.ifPresent(h::schema);
                        h.required(header.required());
                        apiResponse.addHeaderObject(header.name(), h);
                    });
                    return new ImmutablePair<>(response.responseCode(), apiResponse);
                }).collect(Collectors.toMap(x -> x.left, x -> x.right)));
        operation.responses(apiResponses);
        Arrays.stream(annotation.parameters()).forEach(parameter -> {
            Parameter p = findAlreadyProcessedParamFromVertxRoute(parameter.name(), operation.getParameters());
            if (p == null) {
                p = new Parameter();
                operation.addParametersItem(p);
            }
            p.name(parameter.name());
            p.description(parameter.description());
            p.allowEmptyValue(parameter.allowEmptyValue());
            try {
                p.style(Parameter.StyleEnum.valueOf(parameter.style().name()));
            } catch (IllegalArgumentException ie) {
                log.warn(ie.getMessage());
            }
            p.setRequired(parameter.required());
            p.in(parameter.in().name().toLowerCase());
            Optional<Schema> schemaFromAnnotation = AnnotationsUtils.getSchemaFromAnnotation(parameter.schema());
            schemaFromAnnotation.ifPresent(p::schema);
        });
    }

    private static Object clean(final String in) {
        return in;
    }

    private static Content getContent(io.swagger.v3.oas.annotations.media.Content content) {
        Content c = new Content();
        MediaType mediaType = new MediaType();
        Optional<Schema> schemaFromAnnotation = AnnotationsUtils.getSchemaFromAnnotation(content.schema());
        schemaFromAnnotation.ifPresent(mediaType::setSchema);
        if (!schemaFromAnnotation.isPresent()) {
            Optional<ArraySchema> arraySchema = AnnotationsUtils.getArraySchema(content.array());
            arraySchema.ifPresent(mediaType::setSchema);
        }
        c.addMediaType(content.mediaType(), mediaType);
        return c;
    }

    private static Parameter findAlreadyProcessedParamFromVertxRoute(final String name, List<Parameter> parameters) {
        for (Parameter parameter : parameters) {
            if (name.equals(parameter.getName()))
                return parameter;
        }
        return null;
    }

    static io.swagger.v3.oas.models.parameters.RequestBody fromRequestBody(RequestBody body) {
        io.swagger.v3.oas.models.parameters.RequestBody rb = new io.swagger.v3.oas.models.parameters.RequestBody();
        rb.setDescription(body.description());
        if (body.content().length == 1) {
            Content c = getContent(body.content()[0]);
            io.swagger.v3.oas.annotations.media.Content content = body.content()[0];
            if (!Void.class.equals(content.array().schema().implementation()))
                c.get(content.mediaType()).getSchema().setExample(clean(content.array().schema().example()));
            else if (!Void.class.equals(content.schema().implementation()))
                c.get(content.mediaType()).getSchema().setExample(content.schema().example());
            rb.setContent(c);
        }
        return rb;
    }
}

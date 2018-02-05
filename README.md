# Vertx-OpenApi-Spec-Generator
The purpose of this repository is to facilitate the automatic generation of OpenAPI 3 spec from "rest-like" vertx routes. The functional nature of vertx
makes this job difficult thus no serious official tool exists at the moment. In order to mitigate the problem we try to describe the 
general algorithm and assumptions made:

1. Generation of OpenAPI 3 Spec is generated at runtime by introspection of the vertx Router Object which contains the routes. If someone wishes
to perform generation at build time, a possible solution is to generate the route in a unit test and create the spec during test execution.
2. Each vertx route that needs to take part in the spec must contain at most one handler that delegates to a method that is decorated with
an 'io.swagger.v3.oas.annotations.Operation' annotation. The handler itself must not close in nothing but the routing context. This will aid
the introspection of the actual type located in the JVM synthetic class of the handler which actually contains the annotated method.
That implies that a route can have many intermediate handlers depending on the use case and still express the overall result through an
'io.swagger.v3.oas.annotations.Operation' annotation from the selected handler.
3. All io.swagger.v3.oas.annotations.Operation' annotations must contain the 'method' attribute in order to be able to be cross-matched with the route
definition (e.g route.head(....) -> annotation in method must have the 'HEAD' value)
4. The generator at the moment tries to focus on Operations and just provide the basics for other parts of the spec (Info, Contacts, Serves) since
most of them are configuration specific.
5. Mappings between annotations and OpenAPI 3 model are missing but the overall concept makes extensions easily adjusted.
6. NO JAX-RS annotations must be used.

# Example Case
An actual application of these functions can be found at:

https://github.com/ckaratzas/tus-server-implementation/blob/master/src/main/java/com/tus/oss/server/core/ServerVerticle.java for generating the spec
and
https://github.com/ckaratzas/tus-server-implementation/blob/master/src/main/java/com/tus/oss/server/core/*Handler.java for the usage of annotations in handlers.

# Current Status
By no means the whole OpenAPI 3 spec is covered. The current codebase can be extended based on the actual use cases and support more OpenAPI specification.
This repository can be used as a basis to inspire the design of official vertx "openapi-enabled" vertx routes in order to make the introspection easier and
more effective. 
 


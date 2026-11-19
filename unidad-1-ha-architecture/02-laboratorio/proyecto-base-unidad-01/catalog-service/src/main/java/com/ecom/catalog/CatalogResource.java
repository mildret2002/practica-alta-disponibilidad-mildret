package com.ecom.catalog;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Random;
import java.util.logging.Logger;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

@Path("/v1/products")
public class CatalogResource {
    private static final Logger LOG = Logger.getLogger(CatalogResource.class.getName());
    private static final ThreadLocal<Integer> ATTEMPT_COUNT =
            ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Boolean> SHOULD_FAIL =
            ThreadLocal.withInitial(() -> false);
    private final Random random = new Random();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Timeout(800)
    @Retry(maxRetries = 2, delay = 150)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.7, delay = 5000)
    @Fallback(fallbackMethod = "getCatalogFallback")
    public Response getProducts() throws InterruptedException {
        int attempt = ATTEMPT_COUNT.get() + 1;
        ATTEMPT_COUNT.set(attempt);
        if (attempt == 1) {
            SHOULD_FAIL.set(random.nextInt(10) < 6);
        }

        LOG.info("[Intento " + attempt + "/3] Consultando productos");
        if (SHOULD_FAIL.get()) {
            if (attempt == 2) {
                LOG.warning("Simulando una demora de 3 segundos");
                Thread.sleep(3000);
            } else {
                LOG.severe("Falla de conexion simulada en el intento " + attempt);
                throw new RuntimeException("Database connection timeout");
            }
        }

        ATTEMPT_COUNT.remove();
        SHOULD_FAIL.remove();
        return Response.ok(
                "{\"status\":\"SUCCESS\",\"data\":[\"Product A\",\"Product B\",\"Product C\"]}"
        ).build();
    }

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Catalog Service v1.0.0 - Unidad 1 - Alta Disponibilidad";
    }

    public Response getCatalogFallback() {
        int intentosEjecutados = ATTEMPT_COUNT.get();
        ATTEMPT_COUNT.remove();
        SHOULD_FAIL.remove();
        LOG.info("Fallback activado. Intentos ejecutados: " + intentosEjecutados);
        return Response.ok(
                "{\"status\":\"DEGRADED_CACHE\",\"data\":[\"Cached Product A\",\"Cached Product B\"]}"
        ).build();
    }
}



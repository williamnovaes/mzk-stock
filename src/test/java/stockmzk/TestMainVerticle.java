package stockmzk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

	private Vertx vertx;
	
	@BeforeEach
	void deployVerticle(VertxTestContext testContext) {
		vertx = Vertx.vertx();
		vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
	}

	@Test
	void testInserirComJsonInvalido(VertxTestContext testContext) throws Throwable {
		WebClient client = WebClient.create(vertx);
		final JsonObject json = new JsonObject("{ \"nome\" : \"Bermuda\", \"codigo_barras\" :  \"123456\"}");
			
		client.post(8080, "localhost", "/api/incluir")
			.putHeader("content-type", "application-json; charset=utf-8")
			.sendJson(json, testContext.succeeding(response -> {
				testContext.verify(() -> {
				assertThat(response.statusCode()).isEqualTo(400);
				testContext.completeNow();
				});
			}));
	}
	
	@Test
	void testInserirComJsonValido(VertxTestContext testContext) throws Throwable {
		WebClient client = WebClient.create(vertx);
		final JsonArray json = new JsonArray("[{ \"nome\" : \"Bermuda\", \"codigo_barras\" :  \"123456\", \"numero_serie\" : \"1\"}]");
			
		client.post(8080, "localhost", "/api/incluir")
			.putHeader("content-type", "application-json; charset=utf-8")
			.sendJson(json, testContext.succeeding(response -> {
				testContext.verify(() -> {
					assertThat(response.statusCode()).isEqualTo(200);
					assertThat(response.bodyAsString()).contains("Bermuda");
					testContext.completeNow();
				});
			}));
	}
	
	@AfterEach
	void cleanup() {
		vertx.close();
	}
}

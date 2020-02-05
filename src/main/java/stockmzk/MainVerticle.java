package stockmzk;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class MainVerticle extends AbstractVerticle {
	private final String CONTENT_TYPE = "Content-type";
	private final String APPLICATION_JSON = "application-json; charset=utf-8";
	
	private List<JsonObject> produtos;
	
	@Override
	public void start(Promise<Void> promise) throws Exception {
		produtos = new ArrayList<>();
		
		Router router = Router.router(vertx);
		
		router.route("/").handler(routingContext -> {
			HttpServerResponse response = routingContext.response();
			response.putHeader("content-type", "text/html")
			.end("<h1>Hello to MZK Stock</h1>");
		});
		
		router.route("/api*").handler(BodyHandler.create());
		router.get("/api/listar").handler(this::listar);
		router.post("/api/incluir").handler(this::incluir);
		router.post("/api/baixar").handler(this::baixar);
		
		vertx.createHttpServer()
			.requestHandler(router)
			.listen(config().getInteger("http.port", 8080), 
				result -> {
					if (result.succeeded()) {
						promise.complete();
					} else {
						promise.fail(result.cause());
					}
				});
	}
	
	private void listar(RoutingContext context) {
		if (produtos == null || produtos.size() == 0) {
			response(context, getMensagemRetorno("Não há produtos disponíveis"));
		} else {
			response(context, Json.encode(produtos));
		}
	}
	
	private void incluir(RoutingContext context) {
		List<JsonObject> produtosAdicionar = validateJson(context);
		
		if (produtosAdicionar != null && produtosAdicionar.size() > 0) {
			for (int i = 0; i < produtosAdicionar.size(); i++) {
				if (verificarProdutoJaAdicionado(produtosAdicionar.get(i)) == null) {
					produtos.add(produtosAdicionar.get(i));
				}
			}
			response(context, getMensagemRetorno("Produtos incluídos com sucesso"));
		}
	}
	
	private void baixar(RoutingContext context) {
		try {
			List<JsonObject> jsonProdutos = validateJson(context);
			
			List<Integer> indicesRemover = new ArrayList<>();
			if (jsonProdutos != null && jsonProdutos.size() > 0) {
				for (int i = 0; i < jsonProdutos.size(); i++) { 
					Integer indice = verificarProdutoJaAdicionado(jsonProdutos.get(i));
					if (indice != null) {
						indicesRemover.add(indice);
					}
				}
			}
			
			for (int indice : indicesRemover) {
				produtos.remove(indice);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		response(context, getMensagemRetorno("Produtos baixados com sucesso"));
	}
	
	private Integer verificarProdutoJaAdicionado(JsonObject produto) {
		for (int i = 0; i < produtos.size(); i++) {
			if (produtos.get(i).getString("codigo_barras").equals(produto.getString("codigo_barras"))
					&& produtos.get(i).getString("numero_serie").equals(produto.getString("numero_serie"))) {
				return i;
			}
		}
		return null;
	}
	
	private void response(RoutingContext context, String json) {
		context.response()
			.setStatusCode(HttpResponseStatus.OK.code())
			.putHeader(CONTENT_TYPE, APPLICATION_JSON)
			.end(json);
	}
	
	private List<JsonObject> validateJson(RoutingContext context) {
		List<JsonObject> produtosJson = new ArrayList<>();
		try {
			JsonArray request = new JsonArray(context.getBodyAsString());
				
			for (Object item : request) {
				JsonObject produto = new JsonObject(item.toString());
				if (produto.containsKey("nome") && isValorValido(produto.getString("nome"))
						&& produto.containsKey("codigo_barras") && isValorValido(produto.getString("codigo_barras"))
						&& produto.containsKey("numero_serie") && isValorValido(produto.getString("numero_serie"))) {
					produtosJson.add(produto);
				} else {
					response(context, getMensagemRetorno("Os produtos devem conter nome, codigo de barras e número de serie"));
				}
			}
		} catch (Exception e) {
			response(context, getMensagemRetorno(e.getMessage()));
		}
		return produtosJson;
	}
	
	private boolean isValorValido(String valor) {
		return valor != null && !valor.isEmpty();
	}
	
	private String getMensagemRetorno(String mensagem) {
		return "{ \"mensagem\" : \"" +  mensagem + "\" }";
	}
}
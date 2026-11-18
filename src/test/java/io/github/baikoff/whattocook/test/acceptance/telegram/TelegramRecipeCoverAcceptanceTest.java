package io.github.baikoff.whattocook.test.acceptance.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.baikoff.whattocook.telegram.polling.TelegramLongPollingRunner;
import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TelegramRecipeCoverAcceptanceTest extends AbstractIntegrationTest {

	private static final FakeTelegramApi TELEGRAM_API = new FakeTelegramApi();

	@Autowired
	private TelegramLongPollingRunner pollingRunner;

	@DynamicPropertySource
	static void telegramProperties(DynamicPropertyRegistry registry) {
		registry.add("app.telegram.enabled", () -> true);
		registry.add("app.telegram.token", () -> "test-token");
		registry.add("app.telegram.allowed-user-ids", () -> "42");
		registry.add("app.telegram.api-base-url", TELEGRAM_API::baseUrl);
	}

	@AfterAll
	void stopTelegramApi() {
		pollingRunner.stop();
		TELEGRAM_API.stop();
	}

	@Test
	@DisplayName("Рекомендация с обложкой отправляется в Telegram как фотография")
	void recommendationWithCoverIsSentAsPhoto() throws InterruptedException {
		assertThat(TELEGRAM_API.awaitPhoto(Duration.ofSeconds(5)))
				.as("Бот должен загрузить обложку рецепта через метод sendPhoto")
				.isTrue();

		assertThat(TELEGRAM_API.photoContentType())
				.startsWith("multipart/form-data;")
				.contains("boundary=");
		assertThat(TELEGRAM_API.photoRequestBody())
				.contains("name=\"chat_id\"")
				.contains("42")
				.contains("name=\"caption\"")
				.contains("Классические сырники")
				.contains("name=\"photo\"")
				.contains("filename=")
				.contains("CHANGE_PRODUCTS");
	}

	private static final class FakeTelegramApi {

		private static final byte[] SELECT_PRODUCTS_CALLBACK = jsonBytes("""
				{"ok":true,"result":[{
				  "update_id":4001,
				  "callback_query":{
				    "id":"select-products-callback-99",
				    "from":{"id":42,"is_bot":false,"first_name":"Артём"},
				    "message":{
				      "message_id":20,
				      "chat":{"id":42,"type":"private","first_name":"Артём"},
				      "date":1795000001,
				      "text":"Что приготовить?"
				    },
				    "chat_instance":"chat-instance-42",
				    "data":"SELECT_PRODUCTS"
				  }
				}]}
				""");
		private static final byte[] PRODUCTS_MESSAGE = jsonBytes("""
				{"ok":true,"result":[{
				  "update_id":4002,
				  "message":{
				    "message_id":21,
				    "from":{"id":42,"is_bot":false,"first_name":"Артём"},
				    "chat":{"id":42,"type":"private","first_name":"Артём"},
				    "date":1795000002,
				    "text":"творог, ванильный сахар"
				  }
				}]}
				""");
		private static final byte[] EMPTY_UPDATES = jsonBytes("{\"ok\":true,\"result\":[]}");
		private static final byte[] SUCCESS = jsonBytes("{\"ok\":true,\"result\":true}");

		private final HttpServer server;
		private final AtomicInteger updateNumber = new AtomicInteger();
		private final CountDownLatch photoSent = new CountDownLatch(1);
		private volatile String photoContentType;
		private volatile String photoRequestBody;

		private FakeTelegramApi() {
			try {
				server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
				server.createContext("/bottest-token/getUpdates", this::handleGetUpdates);
				server.createContext("/bottest-token/answerCallbackQuery", this::handleSuccess);
				server.createContext("/bottest-token/sendMessage", this::handleSuccess);
				server.createContext("/bottest-token/sendPhoto", this::handleSendPhoto);
				server.start();
			} catch (IOException exception) {
				throw new IllegalStateException("Не удалось запустить поддельный Telegram API", exception);
			}
		}

		private String baseUrl() {
			return "http://127.0.0.1:" + server.getAddress().getPort();
		}

		private boolean awaitPhoto(Duration timeout) throws InterruptedException {
			return photoSent.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
		}

		private String photoContentType() {
			return photoContentType;
		}

		private String photoRequestBody() {
			return photoRequestBody;
		}

		private void stop() {
			server.stop(0);
		}

		private void handleGetUpdates(HttpExchange exchange) throws IOException {
			var response = switch (updateNumber.getAndIncrement()) {
				case 0 -> SELECT_PRODUCTS_CALLBACK;
				case 1 -> PRODUCTS_MESSAGE;
				default -> EMPTY_UPDATES;
			};
			respond(exchange, response);
		}

		private void handleSendPhoto(HttpExchange exchange) throws IOException {
			photoContentType = exchange.getRequestHeaders().getFirst("Content-Type");
			photoRequestBody = readRequestBody(exchange);
			photoSent.countDown();
			respond(exchange, SUCCESS);
		}

		private void handleSuccess(HttpExchange exchange) throws IOException {
			readRequestBody(exchange);
			respond(exchange, SUCCESS);
		}

		private String readRequestBody(HttpExchange exchange) throws IOException {
			return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		}

		private void respond(HttpExchange exchange, byte[] body) throws IOException {
			exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
			exchange.sendResponseHeaders(200, body.length);
			try (var responseBody = exchange.getResponseBody()) {
				responseBody.write(body);
			}
		}

		private static byte[] jsonBytes(String json) {
			return json.getBytes(StandardCharsets.UTF_8);
		}
	}
}

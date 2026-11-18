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
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TelegramSurpriseAcceptanceTest extends AbstractIntegrationTest {

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
	@DisplayName("Кнопка «Удиви меня» подтверждается и присылает случайный рецепт")
	void surpriseCallbackIsAnsweredWithRecipe() throws InterruptedException {
		assertThat(TELEGRAM_API.awaitCallbackAnswer(Duration.ofSeconds(5)))
				.as("Бот должен подтвердить callback, чтобы Telegram убрал индикатор загрузки")
				.isTrue();
		assertThat(TELEGRAM_API.awaitOutgoingMessage(Duration.ofSeconds(5)))
				.as("Бот должен прислать рекомендацию после нажатия «Удиви меня»")
				.isTrue();

		assertThat(TELEGRAM_API.callbackAnswerBody())
				.contains("\"callback_query_id\":\"surprise-callback-77\"");
		assertThat(TELEGRAM_API.outgoingMessageBody())
				.contains("42")
				.contains("🍽")
				.contains("Сложность:")
				.contains("мин")
				.contains("Удиви ещё раз")
				.contains("SURPRISE");
	}

	private static final class FakeTelegramApi {

		private static final byte[] SURPRISE_CALLBACK = ("""
				{"ok":true,"result":[{
				  "update_id":2001,
				  "callback_query":{
				    "id":"surprise-callback-77",
				    "from":{"id":42,"is_bot":false,"first_name":"Артём"},
				    "message":{
				      "message_id":8,
				      "chat":{"id":42,"type":"private","first_name":"Артём"},
				      "date":1794916801,
				      "text":"Что приготовить?"
				    },
				    "chat_instance":"chat-instance-42",
				    "data":"SURPRISE"
				  }
				}]}
				""").getBytes(StandardCharsets.UTF_8);
		private static final byte[] EMPTY_UPDATES =
				"{\"ok\":true,\"result\":[]}".getBytes(StandardCharsets.UTF_8);
		private static final byte[] SUCCESS =
				"{\"ok\":true,\"result\":true}".getBytes(StandardCharsets.UTF_8);

		private final HttpServer server;
		private final AtomicBoolean callbackServed = new AtomicBoolean();
		private final CountDownLatch callbackAnswered = new CountDownLatch(1);
		private final CountDownLatch outgoingMessage = new CountDownLatch(1);
		private volatile String callbackAnswerBody;
		private volatile String outgoingMessageBody;

		private FakeTelegramApi() {
			try {
				server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
				server.createContext("/bottest-token/getUpdates", this::handleGetUpdates);
				server.createContext("/bottest-token/answerCallbackQuery", this::handleCallbackAnswer);
				server.createContext("/bottest-token/sendMessage", this::handleSendMessage);
				server.createContext("/bottest-token/sendPhoto", this::handleSendMessage);
				server.start();
			} catch (IOException exception) {
				throw new IllegalStateException("Не удалось запустить поддельный Telegram API", exception);
			}
		}

		private String baseUrl() {
			return "http://127.0.0.1:" + server.getAddress().getPort();
		}

		private boolean awaitCallbackAnswer(Duration timeout) throws InterruptedException {
			return callbackAnswered.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
		}

		private boolean awaitOutgoingMessage(Duration timeout) throws InterruptedException {
			return outgoingMessage.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
		}

		private String callbackAnswerBody() {
			return callbackAnswerBody;
		}

		private String outgoingMessageBody() {
			return outgoingMessageBody;
		}

		private void stop() {
			server.stop(0);
		}

		private void handleGetUpdates(HttpExchange exchange) throws IOException {
			var body = callbackServed.compareAndSet(false, true) ? SURPRISE_CALLBACK : EMPTY_UPDATES;
			respond(exchange, body);
		}

		private void handleCallbackAnswer(HttpExchange exchange) throws IOException {
			callbackAnswerBody = readRequestBody(exchange);
			callbackAnswered.countDown();
			respond(exchange, SUCCESS);
		}

		private void handleSendMessage(HttpExchange exchange) throws IOException {
			outgoingMessageBody = readRequestBody(exchange);
			outgoingMessage.countDown();
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
	}
}

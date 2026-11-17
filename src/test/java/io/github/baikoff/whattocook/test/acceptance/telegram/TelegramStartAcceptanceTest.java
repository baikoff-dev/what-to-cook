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
class TelegramStartAcceptanceTest extends AbstractIntegrationTest {

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
	@DisplayName("Команда /start показывает приветствие и кнопку «Удиви меня»")
	void startCommandShowsWelcomeAndSurpriseButton() throws InterruptedException {
		assertThat(TELEGRAM_API.awaitOutgoingMessage(Duration.ofSeconds(5)))
				.as("Приложение должно ответить на обновление Telegram")
				.isTrue();

		assertThat(TELEGRAM_API.outgoingMessageBody())
				.contains("\"chat_id\":42")
				.contains("Что приготовить?")
				.contains("Удиви меня")
				.contains("\"callback_data\":\"SURPRISE\"");
	}

	private static final class FakeTelegramApi {

		private static final byte[] START_UPDATE = ("""
				{"ok":true,"result":[{
				  "update_id":1001,
				  "message":{
				    "message_id":7,
				    "from":{"id":42,"is_bot":false,"first_name":"Артём"},
				    "chat":{"id":42,"type":"private","first_name":"Артём"},
				    "date":1794916800,
				    "text":"/start",
				    "entities":[{"offset":0,"length":6,"type":"bot_command"}]
				  }
				}]}
				""").getBytes(StandardCharsets.UTF_8);
		private static final byte[] EMPTY_UPDATES =
				"{\"ok\":true,\"result\":[]}".getBytes(StandardCharsets.UTF_8);
		private static final byte[] SEND_MESSAGE_RESULT = ("""
				{"ok":true,"result":{
				  "message_id":8,
				  "date":1794916801,
				  "chat":{"id":42,"type":"private","first_name":"Артём"},
				  "text":"Что приготовить?"
				}}
				""").getBytes(StandardCharsets.UTF_8);

		private final HttpServer server;
		private final AtomicBoolean startUpdateServed = new AtomicBoolean();
		private final CountDownLatch outgoingMessage = new CountDownLatch(1);
		private volatile String outgoingMessageBody;

		private FakeTelegramApi() {
			try {
				server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
				server.createContext("/bottest-token/getUpdates", this::handleGetUpdates);
				server.createContext("/bottest-token/sendMessage", this::handleSendMessage);
				server.start();
			} catch (IOException exception) {
				throw new IllegalStateException("Не удалось запустить поддельный Telegram API", exception);
			}
		}

		private String baseUrl() {
			return "http://127.0.0.1:" + server.getAddress().getPort();
		}

		private boolean awaitOutgoingMessage(Duration timeout) throws InterruptedException {
			return outgoingMessage.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
		}

		private String outgoingMessageBody() {
			return outgoingMessageBody;
		}

		private void stop() {
			server.stop(0);
		}

		private void handleGetUpdates(HttpExchange exchange) throws IOException {
			var body = startUpdateServed.compareAndSet(false, true) ? START_UPDATE : EMPTY_UPDATES;
			respond(exchange, body);
		}

		private void handleSendMessage(HttpExchange exchange) throws IOException {
			outgoingMessageBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			outgoingMessage.countDown();
			respond(exchange, SEND_MESSAGE_RESULT);
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

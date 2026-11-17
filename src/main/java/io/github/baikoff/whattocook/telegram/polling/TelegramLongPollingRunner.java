package io.github.baikoff.whattocook.telegram.polling;

import io.github.baikoff.whattocook.telegram.client.TelegramBotApiClient;
import io.github.baikoff.whattocook.telegram.service.TelegramUpdateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

public class TelegramLongPollingRunner implements SmartLifecycle {

	private static final Logger LOGGER = LoggerFactory.getLogger(TelegramLongPollingRunner.class);
	private static final long EMPTY_RESULT_DELAY_MILLIS = 100;
	private static final long ERROR_RETRY_DELAY_MILLIS = 2_000;

	private final TelegramBotApiClient client;
	private final TelegramUpdateHandler updateHandler;
	private volatile boolean running;
	private volatile Thread pollingThread;
	private long nextUpdateOffset;

	public TelegramLongPollingRunner(
			TelegramBotApiClient client,
			TelegramUpdateHandler updateHandler) {
		this.client = client;
		this.updateHandler = updateHandler;
	}

	@Override
	public synchronized void start() {
		if (running) {
			return;
		}
		running = true;
		pollingThread = Thread.ofVirtual()
				.name("telegram-long-polling")
				.start(this::poll);
	}

	@Override
	public synchronized void stop() {
		running = false;
		if (pollingThread != null) {
			pollingThread.interrupt();
		}
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public int getPhase() {
		return Integer.MAX_VALUE;
	}

	private void poll() {
		while (running) {
			try {
				var updates = client.getUpdates(nextUpdateOffset);
				for (var update : updates) {
					if (update.hasCallbackQuery()) {
						client.answerCallbackQuery(update.callbackQueryId());
					}
					updateHandler.handle(update).ifPresent(client::sendMessage);
					nextUpdateOffset = Math.max(nextUpdateOffset, update.updateId() + 1);
				}
				if (updates.isEmpty()) {
					pause(EMPTY_RESULT_DELAY_MILLIS);
				}
			} catch (RuntimeException exception) {
				if (running) {
					LOGGER.warn(
							"Не удалось связаться с Telegram API, повтор через 2 секунды ({})",
							exception.getClass().getSimpleName());
					pause(ERROR_RETRY_DELAY_MILLIS);
				}
			}
		}
	}

	private void pause(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
	}
}

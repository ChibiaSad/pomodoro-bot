package org.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        PomBot pomBot = new PomBot();
        telegramBotsApi.registerBot(pomBot);
        new Thread(() -> {
            try {
                pomBot.checkTimer();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}
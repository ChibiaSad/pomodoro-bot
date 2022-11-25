package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

public class PomBot extends TelegramLongPollingBot {

    private final ConcurrentHashMap<UserTimer, Long> userTimeStorage = new ConcurrentHashMap<>();

    enum TimerType{WORK, REST}

    record UserTimer(Instant userTime, TimerType timerType){ }

    @Override
    public String getBotUsername() {
        return "stagSlimeBot";
    }

    @Override
    public String getBotToken() {
        return "5447872977:AAFOJa3a9pFFpJ1kRim9s_e8Ge4BMBYnltw";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(!update.hasMessage() || !update.getMessage().hasText()) return;
        var userID = update.getMessage().getChatId();
        if(update.getMessage().getText().equals("/start")) {
            sendMsg(userID,
                    """
            Таймер для повышения эффективности работы по принципу pomodoro
            Высокая концентрация во время работы и посностью отвлечься от неё во время отдыха
            Для просмотра справки /help.
            """);
            System.out.println(update.getMessage().getChat().getUserName() + " вошел");
            return;
        }
        if(update.getMessage().getText().equals("/help")){
            sendMsg(userID,
                    """
            Вы можете задать 4 параметра таймера по порядку:
            время работы и время отдыха в минутах, количество повторений
            и множитель времени работы.
            Пример ввода "1 1 1 1", но обязательны только первые 2.
            """);
            return;
        }

        if (update.getMessage().getText().matches("[\\d ]+")){
            int count, multiply = 1;
            var message = update.getMessage().getText().split(" ");

            Instant workTime = Instant.now().plus(Long.parseLong(message[0]), ChronoUnit.MINUTES);
            Instant restTime = workTime.plus(Long.parseLong(message[1]), ChronoUnit.MINUTES);

            userTimeStorage.put(new UserTimer(workTime, TimerType.WORK), update.getMessage().getChatId());
            userTimeStorage.put(new UserTimer(restTime, TimerType.REST), update.getMessage().getChatId());
            sendMsg(userID, "Поставил таймер");

            if(message.length > 2){
                count = Integer.parseInt(message[2]);
                if (message.length >= 4) multiply = Integer.parseInt(message[3]);
                for(int i = 2; i <= count; i++){
                    workTime = restTime.plus(Long.parseLong(message[0])*multiply, ChronoUnit.MINUTES);
                    restTime = workTime.plus(Long.parseLong(message[1]), ChronoUnit.MINUTES);

                    userTimeStorage.put(new UserTimer(workTime, TimerType.WORK), update.getMessage().getChatId());
                    userTimeStorage.put(new UserTimer(restTime, TimerType.REST), update.getMessage().getChatId());
                }
            }
            System.out.printf("[%s] Добавлен таймер работы, количество рабочих таймеров %d\n", Instant.now(), userTimeStorage.size());
        }
        else sendMsg(userID, "что?");
    }

    private void sendMsg(Long id, String message) {
        SendMessage msg = new SendMessage();
        msg.setChatId(id);
        msg.setText(message);
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void checkTimer() throws InterruptedException {
        while(true){
            System.out.println("Количество рабочих таймеров " + userTimeStorage.size());
            userTimeStorage.forEach((timer, userId) -> {
                if(Instant.now().isAfter(timer.userTime)){
                    userTimeStorage.remove(timer);
                    switch (timer.timerType){
                        case WORK -> sendMsg(userId, "Поработал - отдыхай");
                        case REST -> sendMsg(userId,"Отдохнул - поработай");
                    }
                }
            });
            Thread.sleep(1000);
        }
    }
}

package com.example.smarthome.server.telegram;

import com.example.smarthome.server.telegram.objects.Message;
import com.example.smarthome.server.telegram.objects.callback.AnswerCallback;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardBuilder;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.util.Iterator;
import java.util.function.Consumer;

public class MessageExecutor {

    private static final Logger log = LoggerFactory.getLogger(MessageExecutor.class);
    private static final Bot bot = Bot.getInstance();

    public static void executeAsync(AnswerCallback callback) {
        executeAsync(callback, null);
    }

    public static void executeAsync(AnswerCallback callback, CallbackAction task) {
        if (callback.getCallbackId() != null) {
            AnswerCallbackQuery answer = new AnswerCallbackQuery();

            answer.setCallbackQueryId(callback.getCallbackId());
            answer.setShowAlert(callback.isAlert());
            answer.setText(callback.getText());

            bot.executeAsync(answer, task);
        }
    }

    /*
    MESSAGE OBJECT
     */
    public static void executeAsync(Message msg, CallbackAction task) {
        executeAsync(msg, task, null);
    }

    public static void executeAsync(Message msg, CallbackAction task,
                                    Consumer<TelegramApiRequestException> errorHandler) {

        log.info("Sending message...");
        if (msg.getMessageId() == 0) {
            SendMessage answer = new SendMessage(String.valueOf(msg.getChatId()), msg.getText());
            answer.setParseMode("HTML");

            bot.executeAsync(answer, task, errorHandler);
        } else {
            EditMessageText answer = new EditMessageText();

            answer.setChatId(String.valueOf(msg.getChatId()));
            answer.setMessageId(msg.getMessageId());
            answer.setText(msg.getText());
            answer.setParseMode("HTML");

            bot.executeAsync(answer, task, errorHandler);
        }
    }

    /*
    INLINE_MESSAGE OBJECT
     */
    public static void executeAsync(InlineKeyboardMessage msg, CallbackAction task) {
        executeAsync(msg, task, null);
    }

    public static void executeAsync(InlineKeyboardMessage msg, CallbackAction task,
                                    Consumer<TelegramApiRequestException> errorHandler) {

        log.info("Sending message...");
        InlineKeyboardBuilder builder = InlineKeyboardBuilder.create(msg.getChatId()).setText(msg.getText());

        if (msg.getButtons() != null) {
            Iterator<CallbackButton> buttons = msg.getButtons().iterator();
            while (buttons.hasNext()) {
                for (int i = 0; i < msg.getNumOfColumns(); i++) {
                    if (buttons.hasNext()) {
                        CallbackButton button = buttons.next();
                        builder.button(button.getText(), button.getCallbackText());
                    } else break;
                }
                builder.endRow().row();
            }
            builder.endRow();
        }

        if (msg.isRemoveButton())
            builder.row().button("Удалить", "remove").endRow();

        builder.row();
        if (msg.isBackButton())
            builder.button("Назад", "back");
        if (msg.isAddButton())
            builder.button("Добавить", "add");
        builder.endRow();

        if (msg.getMessageId() == 0) {
            SendMessage answer = builder.buildNew();
            bot.executeAsync(answer, task, errorHandler);
        } else {
            EditMessageText answer = builder.setMessageId(msg.getMessageId()).buildEdited();
            bot.executeAsync(answer, task, errorHandler);
        }
    }

    public static void deleteAsync(long chatId, int messageId, CallbackAction task) {
        log.info("Removing message...");
        DeleteMessage message = new DeleteMessage(String.valueOf(chatId), messageId);
        bot.executeAsync(message, task);
    }
}

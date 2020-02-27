package com.example.smarthome.server.telegram;

import com.example.smarthome.server.exceptions.MessageNotModified;
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
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.util.Iterator;

public class MessageExecutor {

    private static final Logger log = LoggerFactory.getLogger(MessageExecutor.class);
    private static final Bot bot = Bot.getInstance();

    private static final String notModified = "Bad Request: message is not modified";

    public static void execute(AnswerCallback callback) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery()
                .setCallbackQueryId(callback.getCallbackId())
                .setText(callback.getText())
                .setShowAlert(callback.isAlert());
        try {
            bot.execute(answer);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static void execute(Message msg) {
        log.info("Sending message...");
        if (msg.getMessageId() == 0) {
            SendMessage answer = new SendMessage(msg.getChatId(), msg.getText())
                    .setParseMode("HTML");
            try {
                bot.execute(answer);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            EditMessageText answer = new EditMessageText()
                    .setChatId(msg.getChatId())
                    .setText(msg.getText())
                    .setMessageId(msg.getMessageId())
                    .setParseMode("HTML");
            try {
                bot.execute(answer);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    public static void execute(InlineKeyboardMessage msg) {
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
            try {
                bot.execute(answer);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            EditMessageText answer = builder.setMessageId(msg.getMessageId()).buildEdited();
            try {
                bot.execute(answer);
            } catch (TelegramApiRequestException e) {
                if (e.getApiResponse().startsWith(notModified)) {
                    throw new MessageNotModified();
                }
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }
        }
    }

    public static void delete(long chatId, int messageId) {
        log.info("Removing message...");
        DeleteMessage message = new DeleteMessage(chatId, messageId);
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}

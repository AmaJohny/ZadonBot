package ru.ama.zadon.zadonbot;

import ru.ama.zadon.zadonbot.content.ContentConfig;
import ru.ama.zadon.zadonbot.content.ContentConfigProvider;
import ru.ama.zadon.zadonbot.content.ContentEntry;
import ru.ama.zadon.zadonbot.content.ContentType;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Component
public class TelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger( TelegramBot.class );
    private static final String NON_LETTER_CHAR = "[ !@#$%^&*()_+\\-=`'\"\\\\|/?~.,<>\\[\\]{};:№]";
    public static final String CACHED_FILE_ID_FOR_FILE_MESSAGE = "Cached fileId {} for file {}";

    private final BotProperties botProperties;
    private final ContentConfigProvider contentConfigProvider;
    private final TelegramClient telegramClient;

    private final Map<String, Queue<Integer>> lastMessages = new HashMap<>();

    @Autowired
    public TelegramBot( BotProperties botProperties, ContentConfigProvider contentConfigProvider,
                        TelegramClient telegramClient ) {
        this.botProperties = botProperties;
        this.contentConfigProvider = contentConfigProvider;
        this.telegramClient = telegramClient;
    }

    @Override
    public String getBotToken() {
        return botProperties.getToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @AfterBotRegistration
    public void afterRegistration( BotSession botSession ) {
        LOGGER.info( "Registered bot running state is: {}", botSession.isRunning() );
    }

    @Override
    public void consume( Update update ) {
        if ( update.hasMessage() && update.getMessage().hasText() ) {
            ContentConfig contentConfig = contentConfigProvider.getContentConfig();
            Message message = update.getMessage();
            String messageText = message.getText();
            LOGGER.debug( "Got message: {}", messageText );
            if ( messageText.equals( "/help" ) ) {
                sendHelpMessage( message );
            }
            if ( messageText.equals( "/иди-нахуй" ) ) {
                deleteMessageHistory( message );
            }
            postContentWithLogging( contentConfig, messageText, message );
        }
    }

    private void deleteMessageHistory( Message message ) {
        String chatId = String.valueOf( message.getChatId() );
        Queue<Integer> messageHistory = lastMessages.get( chatId );
        if ( messageHistory != null ) {
            List<Integer> messageIds = new ArrayList<>();
            Integer messageId;
            while ( (messageId = messageHistory.poll()) != null ) {
                messageIds.add( messageId );
            }
            LOGGER.debug( "Deleting {} last messages", messageIds.size() );
            DeleteMessages deleteMessages = DeleteMessages.builder()
                                                          .chatId( chatId )
                                                          .messageIds( messageIds )
                                                          .build();
            try {
                telegramClient.execute( deleteMessages );
            } catch ( TelegramApiException e ) {
                LOGGER.error( "Error while deleting messages", e );
            }
        }

    }

    private void sendHelpMessage( Message message ) {
        Message sentMessage = sendText( message.getChatId(), message.getMessageId(), getHelpMessage() );
        fillMessageHistory( sentMessage );
    }

    //TODO Написать нормальный help
    private String getHelpMessage() {
        Set<String> prompts = contentConfigProvider.getContentConfig().promptedContents().keySet();
        StringBuilder helpMessage = new StringBuilder( "Список команд:\n" )
                                        .append( "/help\n" )
                                        .append( "/иди-нахуй Удалить 20 последних сообщений бота\n" );
        for ( String prompt : prompts ) {
            helpMessage.append( prompt ).append( "\n" );
        }
        return helpMessage.toString();
    }

    private void postContentWithLogging( ContentConfig contentConfig, String messageText, Message message ) {
        ContentEntry promptedEntry = contentConfig.promptedContents().get( messageText );
        // Prompted one is fine, anyway you need at first check message, then check timeout
        if ( promptedEntry != null ) {
            LOGGER.debug( "Message matched prompt \"{}\" of entry \"{}\"", promptedEntry.getPrompt(), promptedEntry.getName() );
            if ( promptedEntry.onTimeout() )
                LOGGER.debug( "Matched entry \"{}\" on timeout", promptedEntry.getName() );
            else
                sendContent( message, promptedEntry );
        } else {
            // Not prompted on the other hand is faster to check for timeout, than for keywords matching
            if ( LOGGER.isDebugEnabled() ) { // isDebugEnabled outside for, because it can be very slow
                // so, if debug enabled we check keywords, than timeout. it is slower, but we will see errors in logs
                checkKeywordsAndPostWithLogging( message, contentConfig );
            } else {
                // and if debug is NOT enabled, then we use faster checks
                // no logging here obviously
                checkKeywordsAndPostWithoutLogging( message, contentConfig );
            }
        }
    }

    private void checkKeywordsAndPostWithoutLogging( Message message, ContentConfig contentConfig ) {
        for ( ContentEntry contentEntry : contentConfig.keywordsContents() ) {
            if ( !contentEntry.onTimeout() &&
                 textContainsKeywords( message.getText(), contentEntry.getKeywords() ) ) {
                // no logging
                sendContent( message, contentEntry );
                break; // не копаем дальше контент после отправки
            }
        }
    }

    private boolean textContainsKeywords( String messageText, Set<String> keywords ) {
        for ( String word : messageText.toLowerCase().split( NON_LETTER_CHAR ) ) {
            if ( keywords.contains( word ) )
                return true;
        }
        return false;
    }

    private void checkKeywordsAndPostWithLogging( Message message, ContentConfig contentConfig ) {
        for ( ContentEntry contentEntry : contentConfig.keywordsContents() ) {
            Set<String> keywords = contentEntry.getKeywords();
            if ( textContainsKeywords( message.getText(), keywords ) ) {
                LOGGER.debug( "Message matched keywords {} of entry \"{}\"", keywords, contentEntry.getName() );
                if ( contentEntry.onTimeout() )
                    LOGGER.debug( "Matched entry \"{}\" on timeout", contentEntry.getName() );
                else {
                    sendContent( message, contentEntry );
                    break; // не копаем дальше контент после отправки
                }
            }
        }
    }

    private void sendContent( Message message, ContentEntry contentEntry ) {
        ContentType type = contentEntry.getType();
        LOGGER.debug( "Sending {} for an entry \"{}\"", type, contentEntry.getName() );
        Message sentMessage = null;
        switch ( type ) {
            case TEXT:
                sentMessage = sendText( message, contentEntry );
                break;
            case IMAGE:
                sentMessage = sendImage( message, contentEntry );
                break;
            case GIF:
                sentMessage = sendGif( message, contentEntry );
                break;
            case AUDIO:
                sentMessage = sendAudio( message, contentEntry );
                break;
            case VOICE:
                sentMessage = sendVoice( message, contentEntry );
                break;
            case VIDEO:
                sentMessage = sendVideo( message, contentEntry );
                break;
            case VIDEO_NOTE:
                sentMessage = sendVideoNote( message, contentEntry );
                break;
            default:
                LOGGER.error( "Not implemented content type: {}", type );
                break;
        }
        fillMessageHistory( sentMessage );
    }

    private void fillMessageHistory( Message sentMessage ) {
        if ( sentMessage != null ) {
            Queue<Integer> messageHistory =
                lastMessages.computeIfAbsent( String.valueOf( sentMessage.getChatId() ),
                                              ( chatId ) -> new CircularFifoQueue<>( 20 ) );
            messageHistory.add( sentMessage.getMessageId() );
        }
    }

    private Message sendText( Message message, ContentEntry contentEntry ) {
        String textToSend;
        try {
            Path filepath = contentEntry.getFilepath();
            if ( filepath == null ) {
                textToSend = contentEntry.getMessage();
            } else {
                textToSend = Utils.getFileContent( filepath );
            }
        } catch ( IOException e ) {
            LOGGER.error( "Error while sending message to chat", e );
            return null;
        }
        return sendText( message.getChatId(), message.getMessageId(), textToSend );
    }

    private Message sendText( Long chatId, Integer messageIdToReply, String textToSend ) {
        SendMessage sendMessage = SendMessage.builder()
                                             .chatId( String.valueOf( chatId ) )
                                             .text( textToSend )
                                             .replyToMessageId( messageIdToReply )
                                             .build();
        Message executed = null;
        try {
            executed = telegramClient.execute( sendMessage );
        } catch ( TelegramApiException e ) {
            LOGGER.error( "Error while sending image to chat", e );
        }
        return executed;
    }

    private Message sendImage( Message message, ContentEntry contentEntry ) {
        String fileId = contentEntry.getFileId();
        Path filepath = contentEntry.getFilepath();
        InputFile inputFile = getInputFile( fileId, filepath );

        SendPhoto sendPhoto = SendPhoto.builder()
                                       .chatId( String.valueOf( message.getChatId() ) )
                                       .caption( contentEntry.getMessage() )
                                       .photo( inputFile )
                                       .replyToMessageId( message.getMessageId() )
                                       .build();
        Message executed = null;
        try {
            executed = telegramClient.execute( sendPhoto );
            if ( fileId == null ) {
                fileId = getMaxSizeFileId( executed.getPhoto() );
                contentEntry.saveFileId( fileId );
                LOGGER.debug( CACHED_FILE_ID_FOR_FILE_MESSAGE, fileId, filepath );
            }
        } catch ( TelegramApiException e ) {
            LOGGER.error( "Error while sending image to chat", e );
        }
        return executed;
    }

    @NotNull
    private static InputFile getInputFile( String fileId, Path filepath ) {
        if ( fileId == null ) {
            LOGGER.debug( "Sending new file to telegram: {}", filepath );
            return new InputFile( filepath.toFile() );
        } else {
            LOGGER.debug( "Used cached fileId {} for file {}", fileId, filepath );
            return new InputFile( fileId );
        }
    }

    private String getMaxSizeFileId( List<PhotoSize> photos ) {
        return photos.stream()
                     .max( Comparator.comparing( PhotoSize::getFileSize ) )
                     .map( PhotoSize::getFileId )
                     .orElse( null );// will never be used
    }

    private Message sendGif( Message message, ContentEntry contentEntry ) {
        String fileId = contentEntry.getFileId();
        Path filepath = contentEntry.getFilepath();
        InputFile inputFile = getInputFile( fileId, filepath );

        SendAnimation sendGif = SendAnimation.builder()
                                             .chatId( String.valueOf( message.getChatId() ) )
                                             .caption( contentEntry.getMessage() )
                                             .animation( inputFile )
                                             .replyToMessageId( message.getMessageId() )
                                             .build();
        Message executed = null;
        try {
            executed = telegramClient.execute( sendGif );
            if ( fileId == null ) {
                fileId = executed.getAnimation().getFileId();
                contentEntry.saveFileId( fileId );
                LOGGER.debug( CACHED_FILE_ID_FOR_FILE_MESSAGE, fileId, filepath );
            }
        } catch ( TelegramApiException e ) {
            LOGGER.error( "Error while sending image to chat", e );
        }
        return executed;
    }

    private Message sendAudio( Message message, ContentEntry contentEntry ) {
        String fileId = contentEntry.getFileId();
        Path filepath = contentEntry.getFilepath();
        InputFile inputFile = getInputFile( fileId, filepath );

        SendAudio sendAudio = SendAudio.builder()
                                       .chatId( String.valueOf( message.getChatId() ) )
                                       .caption( contentEntry.getMessage() )
                                       .audio( inputFile )
                                       .replyToMessageId( message.getMessageId() )
                                       .build();
        Message executed = null;
        try {
            executed = telegramClient.execute( sendAudio );
            if ( fileId == null ) {
                fileId = executed.getAudio().getFileId();
                contentEntry.saveFileId( fileId );
                LOGGER.debug( CACHED_FILE_ID_FOR_FILE_MESSAGE, fileId, filepath );
            }
        } catch ( TelegramApiException e ) {
            LOGGER.error( "Error while sending image to chat", e );
        }
        return executed;
    }

    private Message sendVoice( Message message, ContentEntry contentEntry ) {
        String fileId = contentEntry.getFileId();
        Path filepath = contentEntry.getFilepath();
        InputFile inputFile = getInputFile( fileId, filepath );

        SendVoice sendAudio = SendVoice.builder()
                                       .chatId( String.valueOf( message.getChatId() ) )
                                       .caption( contentEntry.getMessage() )
                                       .voice( inputFile )
                                       .replyToMessageId( message.getMessageId() )
                                       .build();
        Message executed = null;
        try {
            executed = telegramClient.execute( sendAudio );
            if ( fileId == null ) {
                fileId = executed.getVoice().getFileId();
                contentEntry.saveFileId( fileId );
                LOGGER.debug( CACHED_FILE_ID_FOR_FILE_MESSAGE, fileId, filepath );
            }
        } catch ( TelegramApiException e ) {
            LOGGER.error( "Error while sending image to chat", e );
        }
        return executed;
    }

    private Message sendVideo( Message message, ContentEntry contentEntry ) {
        String fileId = contentEntry.getFileId();
        Path filepath = contentEntry.getFilepath();
        InputFile inputFile = getInputFile( fileId, filepath );

        SendVideo sendAudio = SendVideo.builder()
                                       .chatId( String.valueOf( message.getChatId() ) )
                                       .caption( contentEntry.getMessage() )
                                       .video( inputFile )
                                       .replyToMessageId( message.getMessageId() )
                                       .build();
        Message executed = null;
        try {
            executed = telegramClient.execute( sendAudio );
            if ( fileId == null ) {
                fileId = executed.getVideo().getFileId();
                contentEntry.saveFileId( fileId );
                LOGGER.debug( CACHED_FILE_ID_FOR_FILE_MESSAGE, fileId, filepath );
            }
        } catch ( TelegramApiException e ) {
            LOGGER.error( "Error while sending image to chat", e );
        }
        return executed;
    }

    private Message sendVideoNote( Message message, ContentEntry contentEntry ) {
        String fileId = contentEntry.getFileId();
        Path filepath = contentEntry.getFilepath();
        InputFile inputFile = getInputFile( fileId, filepath );

        SendVideoNote sendAudio = SendVideoNote.builder()
                                               .chatId( String.valueOf( message.getChatId() ) )
                                               .videoNote( inputFile )
                                               .replyToMessageId( message.getMessageId() )
                                               .build();
        Message executed = null;
        try {
            executed = telegramClient.execute( sendAudio );
            if ( fileId == null ) {
                fileId = executed.getVideoNote().getFileId();
                contentEntry.saveFileId( fileId );
                LOGGER.debug( CACHED_FILE_ID_FOR_FILE_MESSAGE, fileId, filepath );
            }
        } catch ( TelegramApiException e ) {
            LOGGER.error( "Error while sending image to chat", e );
        }
        return executed;
    }
}

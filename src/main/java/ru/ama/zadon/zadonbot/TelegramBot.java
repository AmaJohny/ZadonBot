package ru.ama.zadon.zadonbot;

import ru.ama.zadon.zadonbot.content.ContentConfig;
import ru.ama.zadon.zadonbot.content.ContentConfigProvider;
import ru.ama.zadon.zadonbot.content.ContentEntry;
import ru.ama.zadon.zadonbot.content.ContentType;

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
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Component
public class TelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger( TelegramBot.class );
    private static final String NON_LETTER_CHAR = "[ !@#$%^&*()_+\\-=`'\"\\\\|/?~0-9.,<>\\[\\]{};:№]";
    public static final String CACHED_FILE_ID_FOR_FILE_MESSAGE = "Cached fileId {} for file {}";

    private final BotProperties botProperties;
    private final ContentConfigProvider contentConfigProvider;
    private final TelegramClient telegramClient;

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

            postContentWithLogging( contentConfig, messageText, message );
        }
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
                checkKeywordsAndPostWithoutLogging(message, contentConfig);
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
        switch ( type ) {
            case TEXT:
                sendText( message, contentEntry );
                break;
            case IMAGE:
                sendImage( message, contentEntry );
                break;
            case GIF:
                sendGif( message, contentEntry );
                break;
            case AUDIO:
                sendAudio( message, contentEntry );
                break;
            case VOICE:
                sendVoice( message, contentEntry );
                break;
            default:
                LOGGER.error( "Not implemented content type: {}", type );
                break;
        }
    }

    private void sendText( Message message, ContentEntry contentEntry ) {
        try {
            String textToSend;
            Path filepath = contentEntry.getFilepath();
            if ( filepath == null ) {
                textToSend = contentEntry.getMessage();
            } else {
                textToSend = Utils.getFileContent( filepath );
            }
            sendText( message.getChatId(), message.getMessageId(), textToSend );
        } catch ( IOException | TelegramApiException e ) {
            LOGGER.error( "Error while sending message to chat", e );
        }
    }

    private void sendText( Long chatId, Integer messageIdToReply, String textToSend ) throws TelegramApiException {
        SendMessage sendMessage = SendMessage.builder()
                                             .chatId( String.valueOf( chatId ) )
                                             .text( textToSend )
                                             .replyToMessageId( messageIdToReply )
                                             .build();
        telegramClient.execute( sendMessage );
    }

    private void sendImage( Message message, ContentEntry contentEntry ) {
        String fileId = contentEntry.getFileId();
        Path filepath = contentEntry.getFilepath();
        InputFile inputFile = getInputFile( fileId, filepath );

        SendPhoto sendPhoto = SendPhoto.builder()
                                       .chatId( String.valueOf( message.getChatId() ) )
                                       .caption( contentEntry.getMessage() )
                                       .photo( inputFile )
                                       .replyToMessageId( message.getMessageId() )
                                       .build();
        try {
            Message executed = telegramClient.execute( sendPhoto );
            if ( fileId == null ) {
                fileId = getMaxSizeFileId( executed.getPhoto() );
                contentEntry.saveFileId( fileId );
                LOGGER.debug( CACHED_FILE_ID_FOR_FILE_MESSAGE, fileId, filepath );
            }
        } catch ( TelegramApiException e ) {
            LOGGER.error( "Error while sending image to chat", e );
        }
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

    private void sendGif( Message message, ContentEntry contentEntry ) {
        String fileId = contentEntry.getFileId();
        Path filepath = contentEntry.getFilepath();
        InputFile inputFile = getInputFile( fileId, filepath );

        SendAnimation sendGif = SendAnimation.builder()
                                             .chatId( String.valueOf( message.getChatId() ) )
                                             .caption( contentEntry.getMessage() )
                                             .animation( inputFile )
                                             .replyToMessageId( message.getMessageId() )
                                             .build();
        try {
            Message executed = telegramClient.execute( sendGif );
            if ( fileId == null ) {
                fileId = executed.getAnimation().getFileId();
                contentEntry.saveFileId( fileId );
                LOGGER.debug( CACHED_FILE_ID_FOR_FILE_MESSAGE, fileId, filepath );
            }
        } catch ( TelegramApiException e ) {
            LOGGER.error( "Error while sending image to chat", e );
        }
    }

    private void sendAudio( Message message, ContentEntry contentEntry ) {
        String fileId = contentEntry.getFileId();
        Path filepath = contentEntry.getFilepath();
        InputFile inputFile = getInputFile( fileId, filepath );

        SendAudio sendAudio = SendAudio.builder()
                                       .chatId( String.valueOf( message.getChatId() ) )
                                       .caption( contentEntry.getMessage() )
                                       .audio( inputFile )
                                       .replyToMessageId( message.getMessageId() )
                                       .build();

        try {
            Message executed = telegramClient.execute( sendAudio );
            if ( fileId == null ) {
                fileId = executed.getAudio().getFileId();
                contentEntry.saveFileId( fileId );
                LOGGER.debug( CACHED_FILE_ID_FOR_FILE_MESSAGE, fileId, filepath );
            }
        } catch ( TelegramApiException e ) {
            LOGGER.error( "Error while sending image to chat", e );
        }
    }

    private void sendVoice( Message message, ContentEntry contentEntry ) {
        String fileId = contentEntry.getFileId();
        Path filepath = contentEntry.getFilepath();
        InputFile inputFile = getInputFile( fileId, filepath );

        SendVoice sendAudio = SendVoice.builder()
                                       .chatId( String.valueOf( message.getChatId() ) )
                                       .caption( contentEntry.getMessage() )
                                       .voice( inputFile )
                                       .replyToMessageId( message.getMessageId() )
                                       .build();

        try {
            Message executed = telegramClient.execute( sendAudio );
            if ( fileId == null ) {
                fileId = executed.getVoice().getFileId();
                contentEntry.saveFileId( fileId );
                LOGGER.debug( CACHED_FILE_ID_FOR_FILE_MESSAGE, fileId, filepath );
            }
        } catch ( TelegramApiException e ) {
            LOGGER.error( "Error while sending image to chat", e );
        }
    }

    private boolean textContainsKeywords( String messageText, Set<String> keywords ) {
        for ( String word : messageText.toLowerCase().split( NON_LETTER_CHAR ) ) {
            if ( keywords.contains( word ) )
                return true;
        }
        return false;
    }
}

package ru.ama.zadon.zadonbot;

import ru.ama.zadon.zadonbot.async.UpdateContentTask;
import ru.ama.zadon.zadonbot.content.ContentConfig;
import ru.ama.zadon.zadonbot.content.pasta.PastaEntry;
import ru.ama.zadon.zadonbot.content.pic.PicEntry;
import ru.ama.zadon.zadonbot.git.GitProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger( TelegramBot.class );
    private static final String NON_LETTER_CHAR = "[ !@#$%^&*()_+\\-=`'\"\\\\|/?~0-9.,<>\\[\\]{};:№]";

    private final BotProperties botProperties;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool( 1 );
    private volatile ContentConfig contentConfig;

    @Autowired
    public TelegramBot( BotProperties botProperties, GitProperties gitProperties ) {
        super( botProperties.getToken() );
        this.botProperties = botProperties;
        UpdateContentTask updateContentTask = new UpdateContentTask( gitProperties, botProperties.getConfigFile(),
                                                                     ( config ) -> contentConfig = config );
        updateContentTask.run();
        schedulePeriodicUpdates( updateContentTask );
    }

    private void schedulePeriodicUpdates( UpdateContentTask updateContentTask ) {
        long period = Utils.toMilliseconds( botProperties.getConfigUpdatePeriod() );
        scheduler.scheduleAtFixedRate( updateContentTask, period, period, TimeUnit.MILLISECONDS );
    }

    @Override
    public String getBotUsername() {
        return botProperties.getUsername();
    }

    @Override
    public void onUpdateReceived( Update update ) {
        if ( update.hasMessage() && update.getMessage().hasText() ) {
            Message message = update.getMessage();
            String messageText = message.getText();
            LOGGER.debug( "Got message: {}", messageText );
            long chatId = message.getChatId();
            Integer messageId = message.getMessageId();

            if ( replyWithPasta( messageText, chatId, messageId ) )
                return;

            if ( replyWithPic( messageText, chatId, messageId ) )
                return;

            //something more
        }
    }

    private boolean replyWithPasta( String messageText, long chatId, Integer messageId ) {
        for ( PastaEntry pasta : contentConfig.getPastas() ) {
            if ( notOnTimeout( pasta.getLastUseTime(), pasta.getTimeout() ) &&
                 textContainsKeywords( messageText, pasta.getKeywords() ) ) {
                try {
                    String fileName = pasta.getFilename();
                    sendMessage( chatId, messageId, Files.readString( Path.of( fileName ) ) );
                } catch ( IOException | TelegramApiException e ) {
                    LOGGER.error( "Error while sending message to chat", e );
                }
                return true;
            }
        }
        return false;
    }

    private boolean notOnTimeout( AtomicLong lastPastedTime, long timeout ) {
        long lastPastedTimeLong = lastPastedTime.get();
        long currentTime = System.currentTimeMillis();
        return currentTime > lastPastedTimeLong + timeout &&
               lastPastedTime.compareAndSet( lastPastedTimeLong, currentTime );
    }

    private boolean textContainsKeywords( String messageText, Set<String> keywords ) {
        for ( String word : messageText.toLowerCase().split( NON_LETTER_CHAR ) ) {
            if ( keywords.contains( word ) )
                return true;
        }
        return false;
    }

    private void sendMessage( Long chatId, Integer messageIdToReply, String textToSend ) throws TelegramApiException {
        SendMessage sendMessage = SendMessage.builder()
                                             .chatId( String.valueOf( chatId ) )
                                             .text( textToSend )
                                             .replyToMessageId( messageIdToReply )
                                             .build();

        execute( sendMessage );
    }

    private boolean replyWithPic( String messageText, long chatId, Integer messageId ) {
        for ( PicEntry pic : contentConfig.getPictures() ) {
            if ( messageText.equals( pic.getPrompt() ) && notOnTimeout( pic.getLastUseTime(), pic.getTimeout() ) ) {
                try {
                    sendPicture( chatId, messageId, pic );
                } catch ( TelegramApiException e ) {
                    LOGGER.error( "Error while sending document to chat", e );
                }
                return true;
            }
        }
        return false;
    }

    private void sendPicture( Long chatId, Integer messageIdToReply, PicEntry pic )
        throws TelegramApiException {
        String fileId = pic.getFileId();
        String filename = pic.getFilename();
        InputFile inputFile;
        if ( fileId == null ) {
            LOGGER.debug( "Sending new file to telegram: {}", filename );
            inputFile = new InputFile( Paths.get( filename ).toFile() );
        } else {
            LOGGER.debug( "Used cached fileId {} for file {}", fileId, filename );
            inputFile = new InputFile( fileId );
        }

        SendPhoto sendPhoto = SendPhoto.builder()
                                       .chatId( String.valueOf( chatId ) )
                                       .caption( pic.getMessage() )
                                       .photo( inputFile )
                                       .replyToMessageId( messageIdToReply )
                                       .build();

        Message executed = execute( sendPhoto );
        if ( fileId == null ) {
            fileId = getMaxSizeFileId( executed.getPhoto() );
            pic.setFileId( fileId );
            LOGGER.debug( "Cached fileId {} for file {}", fileId, filename );
        }
    }

    private String getMaxSizeFileId( List<PhotoSize> photo ) {
        Integer maxSize = Integer.MIN_VALUE;
        String fileId = null;
        for ( PhotoSize photoSize : photo ) {
            Integer fileSize = photoSize.getFileSize();
            if ( fileSize > maxSize ) {
                maxSize = fileSize;
                fileId = photoSize.getFileId();
            }
        }
        return fileId;
    }
}

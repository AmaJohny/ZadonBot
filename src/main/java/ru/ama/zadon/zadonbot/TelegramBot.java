package ru.ama.zadon.zadonbot;

import ru.ama.zadon.zadonbot.content.pasta.PastaEntry;
import ru.ama.zadon.zadonbot.content.pasta.PastaProperties;
import ru.ama.zadon.zadonbot.content.pic.PicEntry;
import ru.ama.zadon.zadonbot.content.pic.PicProperties;
import ru.ama.zadon.zadonbot.yaml.pasta.Pasta;
import ru.ama.zadon.zadonbot.yaml.pasta.Pastas;
import ru.ama.zadon.zadonbot.yaml.YamlParser;
import ru.ama.zadon.zadonbot.yaml.pic.Pic;
import ru.ama.zadon.zadonbot.yaml.pic.Pics;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger( TelegramBot.class );

    private final BotProperties botProperties;
    private final PastaProperties pastaProperties;
    private final PicProperties picProperties;
    private final List<PastaEntry> pastas;
    private final List<PicEntry> pics;
    private final Map<String, String> sentFiles = new ConcurrentHashMap<>();

    @Autowired
    public TelegramBot( BotProperties botProperties, PastaProperties pastaProperties, PicProperties picProperties ) {
        super( botProperties.getToken() );
        this.botProperties = botProperties;
        this.pastaProperties = pastaProperties;
        this.picProperties = picProperties;
        this.pastas = initPastas( pastaProperties );
        this.pics = initPics( picProperties );
    }

    private List<PastaEntry> initPastas( PastaProperties pastaProperties ) {
        Pastas parsedPastas = YamlParser.parsePastas( pastaProperties.getList() );
        List<PastaEntry> pastas = new ArrayList<>();
        for ( Pasta parsedPasta : parsedPastas.getPastas() ) {
            pastas.add( new PastaEntry( parsedPasta.getName(), parsedPasta.getRegex(), parsedPasta.getFilename(),
                                        Utils.toMilliseconds( parsedPasta.getTimeout() ) ) );
        }
        return pastas;
    }

    private List<PicEntry> initPics( PicProperties picProperties ) {
        Pics parsedPastas = YamlParser.parsePics( picProperties.getList() );
        List<PicEntry> pastas = new ArrayList<>();
        for ( Pic parsedPic : parsedPastas.getPics() ) {
            pastas.add( new PicEntry( parsedPic.getName(), parsedPic.getPrompt(), parsedPic.getMessage(),
                                      parsedPic.getFilename(), Utils.toMilliseconds( parsedPic.getTimeout() ) ) );
        }
        return pastas;
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
        for ( PastaEntry pasta : pastas ) {
            if ( messageText.toLowerCase().matches( pasta.getRegex() ) &&
                 notOnTimeout( pasta.getLastUseTime(), pasta.getTimeout() ) ) {
                try {
                    sendMessage( chatId, messageId, readMessageFromFile( pasta.getFilename() ) );
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

    private void sendMessage( Long chatId, Integer messageIdToReply, String textToSend ) throws TelegramApiException {
        SendMessage sendMessage = SendMessage.builder()
                                             .chatId( String.valueOf( chatId ) )
                                             .text( textToSend )
                                             .replyToMessageId( messageIdToReply )
                                             .build();

        execute( sendMessage );
    }

    private String readMessageFromFile( String fileName ) throws IOException {
        return Files.readString( Path.of( pastaProperties.getFilesPath(), fileName ) );
    }

    private boolean replyWithPic( String messageText, long chatId, Integer messageId ) {
        for ( PicEntry pic : pics ) {
            if ( messageText.equals( pic.getPrompt() ) && notOnTimeout( pic.getLastUseTime(), pic.getTimeout() ) ) {
                try {
                    sendDocument( chatId, messageId, pic.getMessage(), pic.getFilename() );
                } catch ( TelegramApiException e ) {
                    LOGGER.error( "Error while sending document to chat", e );
                }
                return true;
            }
        }
        return false;
    }

    private void sendDocument( Long chatId, Integer messageIdToReply, String textToSend, String fileName )
        throws TelegramApiException {
        boolean needSaveFileId = false;
        String cachedFileId = sentFiles.get( fileName );
        InputFile inputFile;
        if ( cachedFileId == null ) {
            needSaveFileId = true;
            inputFile = new InputFile( Paths.get( picProperties.getFilesPath(), fileName ).toFile() );
        } else {
            LOGGER.debug( "Used cached fileId {} for file {}", cachedFileId, fileName );
            inputFile = new InputFile( cachedFileId );
        }

        SendPhoto sendPhoto = SendPhoto.builder()
                                       .chatId( String.valueOf( chatId ) )
                                       .caption( textToSend )
                                       .photo( inputFile )
                                       .replyToMessageId( messageIdToReply )
                                       .build();

        Message executed = execute( sendPhoto );
        if ( needSaveFileId ) {
            cacheFileId( fileName, executed.getPhoto() );
        }
    }

    private void cacheFileId( String fileName, List<PhotoSize> photo ) {
        Integer maxSize = Integer.MIN_VALUE;
        String fileId = null;
        for ( PhotoSize photoSize : photo ) {
            Integer fileSize = photoSize.getFileSize();
            if ( fileSize > maxSize ) {
                maxSize = fileSize;
                fileId = photoSize.getFileId();
            }
        }

        sentFiles.put( fileName, fileId );
        LOGGER.debug( "Cached file {} with fileId: {}", fileName, fileId );
    }
}

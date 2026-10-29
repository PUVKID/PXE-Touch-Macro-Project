package kr.yunseok.touchmacro;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.junit.*;
import static org.junit.Assert.*;

public class ErrorJournalTest {
    @Test public void reopenedFilePreservesErrorAndSingleLineState()throws Exception{
        Path root=Files.createTempDirectory("pxe-journal-");
        try{new ErrorJournal(root.toFile()).append("OCR_CAPTURE_TIMEOUT","복구\n대기");String log=new ErrorJournal(root.toFile()).read();assertTrue(log.contains("OCR_CAPTURE_TIMEOUT"));assertTrue(log.contains("복구 대기"));assertEquals(1,log.split("\n").length);assertTrue(log.matches("(?s)\\d{4}-\\d{2}-\\d{2} .*"));}
        finally{for(File f:root.toFile().listFiles())Files.delete(f.toPath());Files.delete(root);}
    }
    @Test public void longRunningErrorsRotateIntoTwoBoundedUtf8Files()throws Exception{
        Path root=Files.createTempDirectory("pxe-journal-");
        try{ErrorJournal journal=new ErrorJournal(root.toFile());for(int i=0;i<2000;i++)journal.append("OCR_CAPTURE_ERROR_3","한글 상태 "+"복구".repeat(190));File[] files=root.toFile().listFiles();assertEquals(2,files.length);for(File file:files){assertTrue(file.length()<=ErrorJournal.LIMIT);StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(Files.readAllBytes(file.toPath())));}assertTrue(new ErrorJournal(root.toFile()).read().contains("OCR_CAPTURE_ERROR_3"));}
        finally{for(File f:root.toFile().listFiles())Files.delete(f.toPath());Files.delete(root);}
    }
}

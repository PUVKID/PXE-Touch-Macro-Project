package kr.yunseok.touchmacro;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

/** 원문 글자나 화면을 저장하지 않는 크기 제한 오류 기록입니다. */
final class ErrorJournal {
    static final int LIMIT=128*1024;
    private static final Object LOCK=new Object();
    private final File current,previous;
    ErrorJournal(File directory){current=new File(directory,"pxe-errors.txt");previous=new File(directory,"pxe-errors.previous.txt");}
    void append(String kind,String state)throws IOException{synchronized(LOCK){
        String line=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z",Locale.KOREA).format(new Date())+" | "+clean(kind)+" | "+clean(state)+"\n";
        byte[] data=line.getBytes(StandardCharsets.UTF_8);
        if(current.length()+data.length>LIMIT){Files.deleteIfExists(previous.toPath());if(current.exists())Files.move(current.toPath(),previous.toPath());}
        try(FileOutputStream out=new FileOutputStream(current,true)){out.write(data);}
    }}
    private String clean(String value){String s=value==null?"":value.replace('\n',' ').replace('\r',' ');return s.length()>400?s.substring(0,400):s;}
    String read()throws IOException{synchronized(LOCK){
        StringBuilder out=new StringBuilder();for(File file:new File[]{previous,current})if(file.exists())out.append(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));return out.toString();
    }}
}

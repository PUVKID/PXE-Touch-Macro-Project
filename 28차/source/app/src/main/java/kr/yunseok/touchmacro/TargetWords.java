package kr.yunseok.touchmacro;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

/** 앱과 진행 위젯이 같은 즐겨찾기와 첫 탐지 글자를 사용합니다. */
public final class TargetWords {
    private TargetWords() {}
    public static List<String> items(Context c) {
        ArrayList<String> out=new ArrayList<>();
        try{JSONArray a=new JSONArray(Ui.prefs(c).getString("target_words","[]"));for(int i=0;i<a.length();i++){String word=a.getString(i).trim();if(!word.isEmpty()&&!out.contains(word))out.add(word);}}
        catch(org.json.JSONException error){throw new IllegalStateException("저장한 글자 목록을 읽지 못했습니다",error);}
        return out;
    }
    private static void save(Context c,List<String> values){if(!Ui.prefs(c).edit().putString("target_words",new JSONArray(values).toString()).commit())throw new IllegalStateException("자주 쓰는 글자를 저장하지 못했습니다");}
    private static String checked(String word){String value=word==null?"":word.trim();if(value.isEmpty())throw new IllegalArgumentException("찾을 글자를 입력하세요");if(value.length()>200)throw new IllegalArgumentException("글자는 200자 이내로 입력하세요");return value;}
    public static void add(Context c,String word){String value=checked(word);List<String> values=items(c);if(!values.contains(value)){if(values.size()>=30)throw new IllegalArgumentException("자주 쓰는 글자는 최대 30개입니다");values.add(value);save(c,values);}}
    public static void remove(Context c,String word){List<String> values=items(c);if(values.remove(word))save(c,values);}
    public static String current(Context c){MacroService service=MacroService.get();String active=service==null?"":service.activeTargetWord();return !active.isEmpty()?active:Ui.prefs(c).contains("watch_config")?WatchStore.load(c).rule.text:Ui.prefs(c).getString("word","확인");}
    public static void select(Context c,String word){
        String value=checked(word);MacroService service=MacroService.get();
        if(service!=null){service.replaceTargetWord(value);return;}
        WatchConfig next=WatchStore.load(c);next.rule.text=value;next.validate();
        Step single=FloatingController.wordStep(c);single.text=value;single.validate(1);
        if(!Ui.prefs(c).edit().putString("word",value).putString("watch_config",next.json().toString()).commit())throw new IllegalStateException("찾을 글자를 저장하지 못했습니다");
    }
    private static void toast(Context c,Exception error){Toast.makeText(c,error.getMessage(),Toast.LENGTH_LONG).show();}
    private static void overlay(Context c,AlertDialog dialog){Context base=c;while(base instanceof android.content.ContextWrapper&&!(base instanceof android.app.Activity)&&!(base instanceof android.app.Service))base=((android.content.ContextWrapper)base).getBaseContext();if(!(base instanceof android.app.Activity)&&dialog.getWindow()!=null)dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);}
    public static void showEditor(Context source,Runnable changed){
        Context c=Ui.themed(source);MacroService service=MacroService.get();if(service!=null&&service.controller().recording){Toast.makeText(c,"녹화를 먼저 끝내세요",Toast.LENGTH_SHORT).show();return;}
        long generation=service==null?0:service.runRevision();boolean resume=service!=null&&service.running&&!service.paused;boolean[] applied={false};if(resume)service.togglePause();
        LinearLayout body=new LinearLayout(c);body.setOrientation(LinearLayout.VERTICAL);int pad=Ui.dp(c,20);body.setPadding(pad,Ui.dp(c,8),pad,Ui.dp(c,4));
        EditText input=new EditText(c);input.setHint("인식할 글자");input.setContentDescription("변경할 글자 입력");input.setText(current(c));input.setSelectAllOnFocus(true);input.setMaxLines(3);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);body.addView(input,new LinearLayout.LayoutParams(-1,-2));
        CheckBox keep=new CheckBox(c);keep.setText("자주 쓰는 글자로 등록");body.addView(keep);
        TextView note=new TextView(c);note.setText("실행 중이면 새 글자로 처음부터 다시 시작합니다.");note.setTextColor(Ui.muted(c));note.setTextSize(12);body.addView(note);
        AlertDialog dialog=new AlertDialog.Builder(c).setTitle("인식할 글자 변경").setView(body).setPositiveButton("적용",null).setNegativeButton("취소",null).create();overlay(c,dialog);
        dialog.setOnDismissListener(d->{if(!applied[0]&&resume&&service.runRevision()==generation&&service.running&&service.paused)service.togglePause();});
        dialog.setOnShowListener(d->{Ui.font(dialog.getWindow().getDecorView());dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{String word=checked(input.getText().toString());if(keep.isChecked()&&!items(c).contains(word)&&items(c).size()>=30)throw new IllegalArgumentException("자주 쓰는 글자는 최대 30개입니다");select(c,word);if(keep.isChecked())add(c,word);applied[0]=true;if(resume&&service.runRevision()==generation&&service.running&&service.paused)service.togglePause();dialog.dismiss();changed.run();}catch(Exception error){toast(c,error);}});});dialog.show();
    }
    public static void fillShortcuts(Context c,LinearLayout host,Runnable changed){
        host.removeAllViews();host.setOrientation(LinearLayout.VERTICAL);
        LinearLayout heading=new LinearLayout(c);heading.setGravity(android.view.Gravity.CENTER_VERTICAL);host.addView(heading,new LinearLayout.LayoutParams(-1,-2));
        TextView label=new TextView(c);label.setText("자주 쓰는 글자");label.setTextSize(12);label.setTextColor(Ui.muted(c));heading.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        Button manage=Ui.button(c,"등록 · 관리");manage.setTextSize(12);heading.addView(manage,new LinearLayout.LayoutParams(-2,-2));manage.setOnClickListener(v->manage(c,changed));
        HorizontalScrollView scroll=new HorizontalScrollView(c);scroll.setHorizontalScrollBarEnabled(false);host.addView(scroll,new LinearLayout.LayoutParams(-1,-2));LinearLayout row=new LinearLayout(c);scroll.addView(row);
        List<String> words=items(c);String selected=current(c);
        if(words.isEmpty()){TextView empty=new TextView(c);empty.setText("글자를 등록하면 여기에 표시됩니다");empty.setTextSize(12);empty.setTextColor(Ui.muted(c));empty.setPadding(0,0,0,Ui.dp(c,8));row.addView(empty);}
        for(String word:words){Button chip=Ui.button(c,word);chip.setMaxWidth(Ui.dp(c,180));chip.setMaxLines(2);chip.setTextSize(13);chip.setContentDescription("글자 단축키 · "+word);chip.setSelected(word.equals(selected));chip.setTextColor(word.equals(selected)?Ui.accent(c):Ui.ink(c));chip.setBackground(Ui.ripple(c,word.equals(selected)?Ui.soft(c):Ui.card(c),10));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.setMargins(0,0,Ui.dp(c,6),0);row.addView(chip,p);chip.setOnClickListener(v->{try{select(c,word);changed.run();}catch(Exception error){toast(c,error);}});}
        Ui.font(host);
    }
    private static void manage(Context c,Runnable changed){
        List<String> words=items(c);String[] choices=new String[words.size()+1];choices[0]="새 글자 등록";for(int i=0;i<words.size();i++)choices[i+1]="삭제 · "+words.get(i);
        AlertDialog dialog=new AlertDialog.Builder(Ui.themed(c)).setTitle("자주 쓰는 글자 관리").setItems(choices,(d,index)->{if(index==0){register(c,changed);return;}try{remove(c,words.get(index-1));changed.run();}catch(Exception error){toast(c,error);}}).setNegativeButton("닫기",null).create();overlay(c,dialog);dialog.setOnShowListener(d->Ui.font(dialog.getWindow().getDecorView()));dialog.show();
    }
    private static void register(Context c,Runnable changed){
        LinearLayout body=new LinearLayout(c);body.setPadding(Ui.dp(c,20),Ui.dp(c,8),Ui.dp(c,20),0);EditText input=new EditText(c);input.setHint("등록할 글자");input.setContentDescription("등록할 글자 입력");input.setMaxLines(3);body.addView(input,new LinearLayout.LayoutParams(-1,-2));
        AlertDialog dialog=new AlertDialog.Builder(Ui.themed(c)).setTitle("글자 등록").setView(body).setPositiveButton("등록",null).setNegativeButton("취소",null).create();overlay(c,dialog);dialog.setOnShowListener(d->{Ui.font(dialog.getWindow().getDecorView());dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{add(c,input.getText().toString());dialog.dismiss();changed.run();}catch(Exception error){toast(c,error);}});});dialog.show();
    }
}

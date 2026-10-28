package kr.yunseok.touchmacro;

import android.graphics.*;
import android.view.*;
import android.view.accessibility.*;
import android.widget.TextView;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

/** 정지 조건을 바꾸지 않고 실패 직전의 실제 뷰와 접근성 창을 보존합니다. */
final class ControlDiagnostics {
    static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    static View stopView(View view){
        if(view instanceof TextView&&"■".contentEquals(((TextView)view).getText()))return view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=stopView(group.getChildAt(i));if(found!=null)return found;}}
        return null;
    }
    static boolean stopShown(GuidedFlowTest h){
        boolean[] shown={false};h.main(()->{try{View stop=stopView((View)field(h.svc.controller(),"panel"));shown[0]=stop!=null&&stop.isShown();}catch(Exception error){throw new RuntimeException(error);}});return shown[0];
    }
    private static void view(StringBuilder text,String name,View v){
        text.append(name).append('=');if(v==null){text.append("없음\n");return;}
        Rect bounds=new Rect();boolean global=v.getGlobalVisibleRect(bounds);
        text.append(v.getClass().getSimpleName()).append(" visibility=").append(v.getVisibility()).append(" shown=").append(v.isShown()).append(" attached=").append(v.isAttachedToWindow()).append(" size=").append(v.getWidth()).append('x').append(v.getHeight()).append(" alpha=").append(v.getAlpha()).append(" globalVisible=").append(global).append(" bounds=").append(bounds).append(" clickable=").append(v.isClickable()).append(" enabled=").append(v.isEnabled()).append(" important=").append(v.getImportantForAccessibility()).append('\n');
    }
    static String capture(GuidedFlowTest h,String name){
        StringBuilder text=new StringBuilder();
        h.main(()->{try{
            FloatingController c=h.svc.controller();View panel=(View)field(c,"panel");
            text.append("현재 서비스 동일=").append(MacroService.get()==h.svc).append(" destroyed=").append(field(h.svc,"destroyed")).append(" running=").append(h.svc.running).append(" paused=").append(h.svc.paused).append('\n');
            text.append("setup_active=").append(SetupFlow.active(h.a)).append(" setup_stage=").append(SetupFlow.stage(h.a)).append(" controller.visible=").append(field(c,"visible")).append(" captureMode=").append(field(c,"captureMode")).append(" builtRunning=").append(field(c,"builtRunning")).append(" collapsed=").append(c.isCollapsed()).append('\n');
            text.append("scale=").append(Ui.prefs(h.a).getInt("scale",100)).append(" opacity=").append(Ui.prefs(h.a).getInt("opacity",95)).append(" panel_x=").append(Ui.prefs(h.a).getInt("panel_x",-1)).append(" panel_y=").append(Ui.prefs(h.a).getInt("panel_y",-1)).append('\n');
            text.append("windowParams=").append(field(c,"params")).append('\n');view(text,"panel",panel);view(text,"stop",stopView(panel));
        }catch(Exception error){text.append("뷰 진단 실패=").append(error).append('\n');}});
        try{
            for(AccessibilityWindowInfo window:h.auto.getWindows()){
                AccessibilityNodeInfo root=null;
                try{root=window.getRoot();text.append("창 id=").append(window.getId()).append(" type=").append(window.getType()).append(" layer=").append(window.getLayer()).append(" active=").append(window.isActive()).append(" focused=").append(window.isFocused()).append(" root=").append(root!=null).append('\n');if(root!=null)nodes(text,root,0,new int[]{0});}
                finally{if(root!=null)root.recycle();window.recycle();}
            }
            text.append("진행=").append(h.svc.progressText()).append('\n').append(h.svc.logText());
            File folder=h.a.getExternalFilesDir(null);if(folder==null)throw new IOException("검사 산출물 폴더 없음");
            File report=new File(folder,"control-"+name+".txt");try(FileOutputStream out=new FileOutputStream(report)){out.write(text.toString().getBytes(StandardCharsets.UTF_8));}
            Bitmap bitmap=h.auto.takeScreenshot();if(bitmap==null)throw new IOException("검사 화면 캡처 없음");
            try(FileOutputStream out=new FileOutputStream(new File(folder,"control-"+name+".png"))){if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("검사 화면 PNG 저장 실패");}finally{bitmap.recycle();}
        }catch(Exception error){text.append("\n창/화면 진단 실패=").append(error);}
        return text.toString();
    }
    private static void nodes(StringBuilder text,AccessibilityNodeInfo node,int depth,int[] count){
        if(depth>8||count[0]++>=160){text.append("노드 진단 생략: 깊이 8/노드 160 상한\n");return;}
        Rect r=new Rect();node.getBoundsInScreen(r);
        text.append("노드 depth=").append(depth).append(" class=").append(node.getClassName()).append(" text=").append(node.getText()).append(" desc=").append(node.getContentDescription()).append(" visible=").append(node.isVisibleToUser()).append(" clickable=").append(node.isClickable()).append(" enabled=").append(node.isEnabled()).append(" bounds=").append(r).append('\n');
        for(int i=0;i<node.getChildCount()&&count[0]<160;i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null)try{nodes(text,child,depth+1,count);}finally{child.recycle();}}
    }
}

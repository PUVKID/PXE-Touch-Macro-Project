package kr.yunseok.touchmacro;

import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 다음 글자 진행의 명시 선택과 실제 저장·취소 경계를 검사합니다. */
@RunWith(AndroidJUnit4.class) public class PxeAdaptiveSettingsTest {
    final PxeRecognitionSettingsTest base=new PxeRecognitionSettingsTest();
    @Before public void boot()throws Exception{base.boot();}
    @After public void end()throws Exception{base.end();}
    void option(boolean checked){AccessibilityNodeInfo node=base.h.find("다음 글자에 맞춰 진행");assertNotNull("실행 설정에 다음 글자 진행 선택이 없습니다",node);try{assertTrue(node.isCheckable());assertEquals(checked,node.isChecked());assertTrue(node.isEnabled());}finally{node.recycle();}}
    @Test public void adaptiveOptionOnlySavesExplicitChoiceAndPreservesAllManualValues()throws Exception {
        String watch=WatchStore.load(base.h.a).json().toString(),before=Store.load(base.h.a).json().toString();assertFalse(Store.load(base.h.a).json().optBoolean("adaptiveWait",false));
        base.open("settings");option(false);base.h.click("다음 글자에 맞춰 진행");base.h.click("취소");assertEquals(before,Store.load(base.h.a).json().toString());
        base.open("settings");option(false);base.h.click("다음 글자에 맞춰 진행");base.h.click("적용");
        JSONObject saved=Store.load(base.h.a).json();assertTrue(saved.getBoolean("adaptiveWait"));JSONObject original=new JSONObject(before);saved.remove("adaptiveWait");original.remove("adaptiveWait");assertEquals(original.toString(),saved.toString());
        assertEquals(watch,WatchStore.load(base.h.a).json().toString());assertEquals(base.libraryBefore,Store.library(base.h.a).get("인식 설정 검사").json().toString());assertEquals(base.wordSourceBefore,Ui.prefs(base.h.a).getString("word_source",""));
        base.open("settings");option(true);base.h.screenshot("PXE16_다음글자_진행설정.png");base.h.click("다음 글자에 맞춰 진행");base.h.click("취소");assertTrue(Store.load(base.h.a).json().getBoolean("adaptiveWait"));
    }
}

package kr.yunseok.touchmacro;
import android.graphics.*;
import kr.yunseok.touchmacro.MacroService.Hit;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.android.gms.tasks.Tasks;
import org.junit.*;import org.junit.runner.RunWith;import java.lang.reflect.*;import java.util.concurrent.TimeUnit;import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class) public class RecognitionRecovery28Test {
 final GuidedFlowTest h=new GuidedFlowTest();TextRecognizer client;
 @Before public void boot()throws Exception{h.boot();client=TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());set("initialW",1080);set("initialH",360);}
 @After public void end(){if(client!=null)client.close();h.end();}
 void set(String name,int value)throws Exception{Field f=MacroService.class.getDeclaredField(name);f.setAccessible(true);f.setInt(h.svc,value);}
 Text image(String second,int x)throws Exception{Bitmap b=Bitmap.createBitmap(1080,360,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.WHITE);Paint p=new Paint(3);p.setColor(Color.BLACK);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(58);if(x<=0)c.drawText("Adobe"+(x==0?" ":"")+second,100,180,p);else{c.drawText("Adobe",100,180,p);c.drawText(second,x,180,p);}try{Text t=Tasks.await(client.process(InputImage.fromBitmap(b,0)),20,TimeUnit.SECONDS);for(Text.TextBlock block:t.getTextBlocks())for(Text.Line line:block.getLines()){System.out.println("OCR_LINE="+line.getText()+" BOX="+line.getBoundingBox());for(Text.Element e:line.getElements())System.out.println("OCR_ELEMENT="+e.getText()+" BOX="+e.getBoundingBox());}return t;}finally{b.recycle();}}
 Hit find(Text text,Step step)throws Exception{Method m=MacroService.class.getDeclaredMethod("ocrHit",Step.class,Text.class);m.setAccessible(true);return (Hit)m.invoke(h.svc,step,text);}
 Step wanted(){Step s=new Step("TEXT_TAP");s.text="Adobe";s.match="EXACT";s.ignoreCase=false;s.ignoreSpaces=false;return s;}
 @Test public void phraseInsideLongerLineUsesExactWords()throws Exception{Text text=image("Pro 15",0);Step s=wanted();s.text="Adobe Pro";Hit hit=find(text,s);assertNotNull("긴 줄 안의 정확한 문구",hit);assertEquals("Adobe Pro",hit.text);assertTrue(hit.rect.right<450);}
 @Test public void mergedColumnsReturnExactWord()throws Exception{Text text=image("15",0);boolean merged=false;for(Text.TextBlock b:text.getTextBlocks())for(Text.Line line:b.getLines())if(line.getText().contains("Adobe")&&line.getText().contains("15"))merged=true;assertTrue("fixture must reproduce a merged OCR line",merged);Hit hit=find(text,wanted());assertNotNull("독립 단어 Adobe를 줄 전체 비교로 놓침",hit);assertEquals("Adobe",hit.text);assertTrue("다른 열 중앙 터치 금지",hit.rect.centerX()<350);}
 @Test public void adjacentSuffixStaysRejectedAcrossRegionEdge()throws Exception{Text text=image("Pro",-1);Step s=wanted();assertNull("AdobePro의 부분 단어 금지",find(text,s));s.right=26;assertNull("영역으로 Pro만 잘라 Adobe로 바꾸지 않음",find(text,s));}
 @Test public void duplicateLabelsKeepOrder()throws Exception{Text text=image("Adobe",650);Step s=wanted();s.occurrence=2;Hit second=find(text,s);assertNotNull("두 번째 정확한 라벨",second);assertTrue(second.rect.centerX()>600);s.text="어도비";assertNull(find(text,s));s.text="ADOBE";assertNull(find(text,s));}
}

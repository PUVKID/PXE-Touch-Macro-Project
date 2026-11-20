package kr.yunseok.touchmacro;

import java.util.*;
import org.json.*;

/** 화면 색·외형·배치를 읽는 제한된 시각 입력입니다. 원본 좌표 재생은 하지 않습니다. */
public final class BehaviorVisual {
    public static final int MAX_WIDTH=64,MAX_HEIGHT=112,DESCRIPTOR=576,INPUT=1160;
    private BehaviorVisual(){}
    public static final class Frame {
        public final int width,height;
        private final byte[] rgb;
        private Frame detail;
        public Frame(int width,int height,byte[] rgb){this(width,height,rgb,false);}
        private Frame(int width,int height,byte[] rgb,boolean detailed){
            if(width<8||height<8||width>(detailed?256:MAX_WIDTH)||height>(detailed?448:MAX_HEIGHT)||rgb==null||rgb.length!=width*height*3)throw BehaviorExample.error("시각 화면 크기 오류");
            this.width=width;this.height=height;this.rgb=rgb.clone();
        }
        public static Frame fromArgb(int width,int height,int[] pixels){
            if(width<8||height<8||width>32768||height>32768||pixels==null||(long)width*height!=pixels.length)throw BehaviorExample.error("시각 원본 크기 오류");
            Frame frame=scaled(width,height,pixels,MAX_WIDTH,MAX_HEIGHT,false);frame.detail=scaled(width,height,pixels,256,448,true);return frame;
        }
        private static Frame scaled(int width,int height,int[] pixels,int maxWidth,int maxHeight,boolean detailed){double scale=Math.min(1,Math.min(maxWidth/(double)width,maxHeight/(double)height));int w=Math.max(8,(int)Math.round(width*scale)),h=Math.max(8,(int)Math.round(height*scale));byte[] rgb=new byte[w*h*3];
            for(int y=0;y<h;y++)for(int x=0;x<w;x++){int color=pixels[Math.min(height-1,(int)((y+.5)*height/h))*width+Math.min(width-1,(int)((x+.5)*width/w))],at=(y*w+x)*3;rgb[at]=(byte)(color>>>16);rgb[at+1]=(byte)(color>>>8);rgb[at+2]=(byte)color;}return new Frame(w,h,rgb,detailed);}
        public byte[] pixels(){return rgb.clone();}
        public Frame stored(){return new Frame(width,height,rgb);}
        private double value(int x,int y,int c){return (rgb[(Math.max(0,Math.min(height-1,y))*width+Math.max(0,Math.min(width-1,x)))*3+c]&255)/255d;}
        public double[] descriptor(){double[] out=new double[DESCRIPTOR];for(int y=0;y<16;y++)for(int x=0;x<12;x++)for(int c=0;c<3;c++)out[(y*12+x)*3+c]=value((int)((x+.5)*width/12),(int)((y+.5)*height/16),c)*2-1;return out;}
        public JSONObject json(){return BehaviorExample.object("width",width,"height",height,"rgb",Base64.getEncoder().encodeToString(rgb));}
        public static Frame fromJson(JSONObject o)throws JSONException{BehaviorExample.keys(o,"width","height","rgb");String text=BehaviorExample.string(o.get("rgb"));if(text.length()>MAX_WIDTH*MAX_HEIGHT*4)throw BehaviorExample.error("시각 바이트 상한");return new Frame((int)BehaviorExample.integer(o.get("width"),8,MAX_WIDTH),(int)BehaviorExample.integer(o.get("height"),8,MAX_HEIGHT),Base64.getDecoder().decode(text));}
        /** 제어창 영역은 배경 학습과 현재 대상 찾기에서 제외합니다. */
        public Frame exclude(int left,int top,int right,int bottom,int screenWidth,int screenHeight){byte[] copy=rgb.clone();for(int y=0;y<height;y++)for(int x=0;x<width;x++)if((x+.5)*screenWidth/width>=left&&(x+.5)*screenWidth/width<right&&(y+.5)*screenHeight/height>=top&&(y+.5)*screenHeight/height<bottom)Arrays.fill(copy,(y*width+x)*3,(y*width+x)*3+3,(byte)0);Frame out=new Frame(width,height,copy,width>MAX_WIDTH||height>MAX_HEIGHT);if(detail!=null)out.detail=detail.exclude(left,top,right,bottom,screenWidth,screenHeight);return out;}
    }
    public static final class Patch {
        public final int width,height,referenceWidth,referenceHeight;
        private final byte[] rgb;
        public Patch(int width,int height,byte[] rgb){this(width,height,rgb,256,448);}
        public Patch(int width,int height,byte[] rgb,int referenceWidth,int referenceHeight){if(width<4||height<4||width>128||height>128||referenceWidth<8||referenceWidth>256||referenceHeight<8||referenceHeight>448||rgb==null||rgb.length!=768)throw BehaviorExample.error("시각 대상 형식 오류");this.width=width;this.height=height;this.referenceWidth=referenceWidth;this.referenceHeight=referenceHeight;this.rgb=rgb.clone();double variance=0;for(int c=0;c<3;c++){double sum=0,sq=0;for(int i=c;i<rgb.length;i+=3){double v=(rgb[i]&255)/255d;sum+=v;sq+=v*v;}variance+=sq/256-Math.pow(sum/256,2);}if(variance/3<.003)throw BehaviorExample.error("단색 영역은 시각 대상으로 배우지 않습니다");}
        public JSONObject json(){return BehaviorExample.object("width",width,"height",height,"reference_width",referenceWidth,"reference_height",referenceHeight,"rgb",Base64.getEncoder().encodeToString(rgb));}
        public static Patch fromJson(JSONObject o)throws JSONException{BehaviorExample.keys(o,"width","height","reference_width","reference_height","rgb");String raw=BehaviorExample.string(o.get("rgb"));if(raw.length()!=1024)throw BehaviorExample.error("시각 대상 바이트 상한");return new Patch((int)BehaviorExample.integer(o.get("width"),4,128),(int)BehaviorExample.integer(o.get("height"),4,128),Base64.getDecoder().decode(raw),(int)BehaviorExample.integer(o.get("reference_width"),8,256),(int)BehaviorExample.integer(o.get("reference_height"),8,448));}
    }
    private static byte[] sample(Frame frame,int left,int top,int width,int height){byte[] rgb=new byte[768];for(int y=0;y<16;y++)for(int x=0;x<16;x++)for(int c=0;c<3;c++)rgb[(y*16+x)*3+c]=(byte)Math.round(frame.value(left+(int)((x+.5)*width/16),top+(int)((y+.5)*height/16),c)*255);return rgb;}
    public static Patch patchAt(Frame frame,double x,double y,int screenWidth,int screenHeight){if(frame==null||!Double.isFinite(x)||!Double.isFinite(y)||x<0||y<0||x>=screenWidth||y>=screenHeight)throw BehaviorExample.error("시각 시범 위치 오류");frame=frame.detail!=null?frame.detail:frame;int w=Math.min(20,frame.width),h=Math.min(20,frame.height),left=Math.max(0,Math.min(frame.width-w,(int)(x*frame.width/screenWidth)-w/2)),top=Math.max(0,Math.min(frame.height-h,(int)(y*frame.height/screenHeight)-h/2));return new Patch(w,h,sample(frame,left,top,w,h),frame.width,frame.height);}
    public static final class Match {public final int left,top,right,bottom;public final double error;Match(int l,int t,int r,int b,double e){left=l;top=t;right=r;bottom=b;error=e;}}
    /** 위치 후보를 모두 비교하며 떨어진 동일 외형이 둘이면 거절합니다. */
    public static Match resolve(Frame frame,Patch patch,int screenWidth,int screenHeight){return resolve(frame,patch,screenWidth,screenHeight,()->false);}
    public static Match resolve(Frame frame,Patch patch,int screenWidth,int screenHeight,BehaviorPolicy.Cancel cancel){
        if(cancel==null)throw BehaviorExample.error("시각 탐색 취소 계약 필요");
        if(frame==null||patch==null||screenWidth<1||screenHeight<1)throw BehaviorExample.error("현재 시각 화면 필요");frame=frame.detail!=null?frame.detail:frame;List<Match> candidates=new ArrayList<>();
        // 사전검사도 16×16 저장 표본의 같은 픽셀을 비교합니다.
        for(double scale:new double[]{.8,1,1.2}){int w=Math.max(4,(int)Math.round(patch.width*scale*frame.width/patch.referenceWidth)),h=Math.max(4,(int)Math.round(patch.height*scale*frame.height/patch.referenceHeight));if(w>frame.width||h>frame.height)continue;
            for(int top=0;top<=frame.height-h;top++){if(cancel.cancelled())throw new java.util.concurrent.CancellationException("시각 탐색 취소");for(int left=0;left<=frame.width-w;left++){double coarse=0;for(int gy=0;gy<4;gy++)for(int gx=0;gx<4;gx++)for(int c=0;c<3;c++){double d=frame.value(left+(int)((gx*4+2.5)*w/16),top+(int)((gy*4+2.5)*h/16),c)-(patch.rgb[((gy*4+2)*16+gx*4+2)*3+c]&255)/255d;coarse+=d*d;}if(coarse/48>.06)continue;byte[] rgb=sample(frame,left,top,w,h);double error=0;for(int i=0;i<rgb.length;i++){double d=((rgb[i]&255)-(patch.rgb[i]&255))/255d;error+=d*d;}error/=rgb.length;if(error<=.025){if(candidates.size()>=4096)throw BehaviorExample.error("이미지 후보가 너무 많아 기다립니다");candidates.add(new Match(left,top,left+w,top+h,error));}}}}
        if(cancel.cancelled())throw new java.util.concurrent.CancellationException("시각 탐색 취소");
        if(candidates.isEmpty())throw BehaviorExample.error("배운 이미지가 현재 화면에 없습니다");candidates.sort(Comparator.comparingDouble(m->m.error));Match best=candidates.get(0);double bx=(best.left+best.right)/2d,by=(best.top+best.bottom)/2d;
        for(Match other:candidates)if(Math.hypot((other.left+other.right)/2d-bx,(other.top+other.bottom)/2d-by)>Math.min(best.right-best.left,best.bottom-best.top)*.6)throw BehaviorExample.error("같은 이미지가 여러 곳에 있어 기다립니다");
        return new Match((int)Math.round(best.left*(double)screenWidth/frame.width),(int)Math.round(best.top*(double)screenHeight/frame.height),(int)Math.round(best.right*(double)screenWidth/frame.width),(int)Math.round(best.bottom*(double)screenHeight/frame.height),best.error);
    }
    public static double[] encode(BehaviorExample.Observation observation){if(observation.visual==null)throw BehaviorExample.error("시각 특징 없는 관측");double[] out=new double[INPUT];System.arraycopy(BehaviorFeatures.encode(observation),0,out,0,584);System.arraycopy(observation.visual.descriptor(),0,out,584,DESCRIPTOR);return out;}
    public static double distance(Frame a,Frame b){if(a==null||b==null)return Double.POSITIVE_INFINITY;double[] x=a.descriptor(),y=b.descriptor();double d=0;for(int i=0;i<x.length;i++)d+=Math.pow(x[i]-y[i],2);return d/x.length;}
}

package kr.yunseok.touchmacro;
import org.json.*;
public final class GesturePaths {
 private GesturePaths(){}
 public static JSONObject stroke(JSONArray points,int start,int duration){JSONObject o=new JSONObject();try{o.put("points",points).put("start",start).put("duration",duration);}catch(Exception e){throw new IllegalArgumentException(e);}return o;}
 public static JSONArray point(double x,double y){JSONArray a=new JSONArray();try{a.put(x);a.put(y);}catch(Exception e){throw new IllegalArgumentException("좌표가 유효하지 않습니다");}return a;}
 public static JSONArray forStep(Step s){
  if("GESTURE".equals(s.type))return s.strokes;
  JSONArray result=new JSONArray();
  if("CURVE".equals(s.type)){JSONArray p=new JSONArray();for(int i=0;i<=32;i++){double t=i/32.0,u=1-t;p.put(point(u*u*s.x+2*u*t*s.cx+t*t*s.x2,u*u*s.y+2*u*t*s.cy+t*t*s.y2));}result.put(stroke(p,0,s.duration));}
  else if(s.type.startsWith("PINCH")){
   double from=s.type.equals("PINCH_IN")?s.y2:s.x2,to=s.type.equals("PINCH_IN")?s.x2:s.y2;
   for(int sign:new int[]{-1,1}){JSONArray p=new JSONArray();p.put(point(s.x+sign*from,s.y));p.put(point(s.x+sign*to,s.y));result.put(stroke(p,0,s.duration));}
  }else if("MULTI_TAP".equals(s.type)){
   for(String pair:s.text.split(";")){String[] xy=pair.trim().split(",");if(xy.length!=2)throw new IllegalArgumentException("좌표 형식: 25,50;75,50");JSONArray p=new JSONArray();p.put(point(Double.parseDouble(xy[0]),Double.parseDouble(xy[1])));result.put(stroke(p,0,s.duration));}
  }return result;
 }
 public static void validate(JSONArray strokes){
  if(strokes.length()<1||strokes.length()>20)throw new IllegalArgumentException("동시 경로는 1~20개입니다");
  try{for(int i=0;i<strokes.length();i++){JSONObject s=strokes.getJSONObject(i);int start=s.getInt("start"),duration=s.getInt("duration");if(start<0||duration<1||start+duration>59000)throw new IllegalArgumentException("제스처는 최대 59초입니다");JSONArray points=s.getJSONArray("points");if(points.length()<1||points.length()>512)throw new IllegalArgumentException("경로의 점은 1~512개입니다");for(int k=0;k<points.length();k++){JSONArray p=points.getJSONArray(k);double x=p.getDouble(0),y=p.getDouble(1);if(!Double.isFinite(x)||!Double.isFinite(y)||x<0||x>100||y<0||y>100)throw new IllegalArgumentException("모든 좌표는 0~100%입니다");}}}
  catch(JSONException e){throw new IllegalArgumentException("제스처 형식을 확인하세요",e);}
 }
}

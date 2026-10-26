package kr.yunseok.touchmacro;
import org.json.JSONObject;
/** A numbered, non-overlapping priority rectangle, in screen percentages. */
public final class WatchArea {
 public double left,top,right,bottom;
 public WatchArea(double l,double t,double r,double b){left=l;top=t;right=r;bottom=b;}
 public WatchArea(JSONObject o){this(o.optDouble("left"),o.optDouble("top"),o.optDouble("right"),o.optDouble("bottom"));}
 public JSONObject json(){try{return new JSONObject().put("left",left).put("top",top).put("right",right).put("bottom",bottom);}catch(Exception e){throw new IllegalStateException(e);}}
 public boolean overlaps(WatchArea other){return Math.max(left,other.left)<Math.min(right,other.right)&&Math.max(top,other.top)<Math.min(bottom,other.bottom);}
 public void apply(Step s){s.left=left;s.top=top;s.right=right;s.bottom=bottom;}
}

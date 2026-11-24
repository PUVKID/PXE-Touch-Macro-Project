package kr.yunseok.touchmacro;

import java.io.*;
import java.util.*;

/** OBJ의 실제 정점과 면을 회전·원근 투영하는 Android 비의존 기하입니다. */
public final class PxeGeometry {
    private final float[][] vertices;
    private final int[][] faces;
    private PxeGeometry(float[][] vertices,int[][] faces){this.vertices=vertices;this.faces=faces;}
    public static PxeGeometry read(Reader source)throws IOException {
        BufferedReader reader=new BufferedReader(source);List<float[]> vertices=new ArrayList<>();List<int[]> faces=new ArrayList<>();String line;
        try{
            while((line=reader.readLine())!=null){
                if(line.length()>4096)throw new IOException("모델 행이 너무 깁니다");
                line=line.trim();if(line.isEmpty()||line.startsWith("#"))continue;String[] fields=line.split("\\s+");
                if(fields[0].equals("v")){
                    if(fields.length!=4||vertices.size()>=1024)throw new IOException("모델 정점 범위를 확인하세요");
                    float[] point=new float[3];for(int i=0;i<3;i++){point[i]=Float.parseFloat(fields[i+1]);if(!Float.isFinite(point[i])||Math.abs(point[i])>10)throw new IOException("모델 좌표 범위를 확인하세요");}vertices.add(point);
                }else if(fields[0].equals("f")){
                    if(fields.length<4||fields.length>9||faces.size()>=1024)throw new IOException("모델 면 범위를 확인하세요");
                    int[] face=new int[fields.length-1];for(int i=0;i<face.length;i++){face[i]=Integer.parseInt(fields[i+1])-1;if(face[i]<0||face[i]>=vertices.size())throw new IOException("모델 면의 정점 번호가 잘못됐습니다");}faces.add(face);
                }else throw new IOException("지원하지 않는 모델 항목입니다");
            }
        }catch(NumberFormatException e){throw new IOException("모델 숫자를 읽지 못했습니다",e);}
        if(vertices.size()<3||faces.isEmpty())throw new IOException("모델의 정점과 면이 없습니다");
        return new PxeGeometry(vertices.toArray(new float[0][]),faces.toArray(new int[0][]));
    }
    public static final class Surface {
        public final float[] x,y;
        public final float depth,light;
        Surface(float[] x,float[] y,float depth,float light){this.x=x;this.y=y;this.depth=depth;this.light=light;}
    }
    public List<Surface> project(float yaw,float pitch){
        if(!Float.isFinite(yaw)||!Float.isFinite(pitch))throw new IllegalArgumentException("회전 값이 유한하지 않습니다");
        float cy=(float)Math.cos(yaw),sy=(float)Math.sin(yaw),cx=(float)Math.cos(pitch),sx=(float)Math.sin(pitch);float[][] rotated=new float[vertices.length][3];
        for(int i=0;i<vertices.length;i++){float[] v=vertices[i];float x=v[0]*cy+v[2]*sy,z=-v[0]*sy+v[2]*cy;rotated[i][0]=x;rotated[i][1]=v[1]*cx-z*sx;rotated[i][2]=v[1]*sx+z*cx;}
        List<Surface> out=new ArrayList<>(faces.length);
        for(int[] face:faces){
            float[] a=rotated[face[0]],b=rotated[face[1]],c=rotated[face[2]];float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
            float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,length=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
            float light=length==0?.28f:.28f+.72f*Math.max(0,(-.35f*nx+.65f*ny+.68f*nz)/length);light=Math.min(1f,light);
            float[] x=new float[face.length],y=new float[face.length];float depth=0;
            for(int j=0;j<face.length;j++){float[] v=rotated[face[j]];float perspective=5f/Math.max(1f,5f-v[2]);x[j]=v[0]*perspective;y[j]=-v[1]*perspective;depth+=v[2];}
            out.add(new Surface(x,y,depth/face.length,light));
        }
        out.sort(Comparator.comparingDouble(surface->surface.depth));return Collections.unmodifiableList(out);
    }
}

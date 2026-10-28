package kr.yunseok.touchmacro;

import org.junit.Test;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

/** 실제 모델의 투영·면 정렬·잘못된 입력 경계를 검사합니다. */
public class PxeGeometryTest {
    Class<?> geometry() throws Exception {
        try { return Class.forName("kr.yunseok.touchmacro.PxeGeometry"); }
        catch (ClassNotFoundException e) { throw new AssertionError("실제 OBJ 투영 기하가 아직 없습니다",e); }
    }
    Object read(String obj) throws Exception { return geometry().getMethod("read",Reader.class).invoke(null,new StringReader(obj)); }
    List<?> surfaces(Object model,float yaw,float pitch)throws Exception{return (List<?>)geometry().getMethod("project",float.class,float.class).invoke(model,yaw,pitch);}
    float number(Object owner,String name)throws Exception{return owner.getClass().getField(name).getFloat(owner);}
    @Test public void perspectivePreservesTriangleAndOrdersFarFacesFirst()throws Exception {
        Object model=read("v -1 -1 -1\nv 1 -1 -1\nv 0 1 -1\nv -1 -1 1\nv 1 -1 1\nv 0 1 1\nf 1 2 3\nf 4 5 6\n");
        List<?> faces=surfaces(model,0,0);assertEquals(2,faces.size());
        assertTrue(number(faces.get(0),"depth")<number(faces.get(1),"depth"));
        float[] far=(float[])faces.get(0).getClass().getField("x").get(faces.get(0));
        float[] near=(float[])faces.get(1).getClass().getField("x").get(faces.get(1));
        assertTrue("가까운 면의 원근 확대",Math.abs(near[0])>Math.abs(far[0]));
        for(Object face:faces){assertEquals(3,((float[])face.getClass().getField("x").get(face)).length);assertTrue(number(face,"light")>=.28f&&number(face,"light")<=1f);}
    }
    @Test public void rotationChangesRealProjectedVertices()throws Exception {
        Object model=read("v -1 -1 0\nv 1 -1 0\nv 0 1 1\nf 1 2 3\n");
        float[] a=(float[])surfaces(model,0,0).get(0).getClass().getField("x").get(surfaces(model,0,0).get(0));
        Object rotated=surfaces(model,.6f,.3f).get(0);float[] b=(float[])rotated.getClass().getField("x").get(rotated);
        assertFalse("정점 회전이 실제 투영에 반영되어야 합니다",Arrays.equals(a,b));
    }
    @Test public void suppliedObjIsNonFlatAndProducesFiniteLitFaces()throws Exception {
        // 순수 검사 root와 Gradle의 app 모듈 cwd를 명시적으로 구분하며 다른 자산을 탐색하지 않습니다.
        Path cwd=Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path module=cwd.getFileName().toString().equals("app")?cwd:cwd.resolve("app");
        String obj=new String(Files.readAllBytes(module.resolve("src/main/assets/pxe_beacon.obj")),StandardCharsets.UTF_8);Object model=read(obj);List<?> faces=surfaces(model,-.42f,-.3f);
        assertTrue("작은 상징물도 실제 다면체여야 합니다",faces.size()>=30);Set<Float> depths=new HashSet<>();
        for(Object face:faces){depths.add(number(face,"depth"));for(String axis:new String[]{"x","y"})for(float v:(float[])face.getClass().getField(axis).get(face))assertTrue(Float.isFinite(v)&&Math.abs(v)<3);}
        assertTrue("모델의 깊이가 실제로 달라야 합니다",depths.size()>4);
    }
    @Test public void invalidFaceAndNonFiniteVertexAreRejected()throws Exception {
        for(String obj:new String[]{"v 0 0 0\nf 1 2 3\n","v NaN 0 0\nf 1 1 1\n"}) {
            try {read(obj);fail("잘못된 OBJ를 허용했습니다");}catch(InvocationTargetException e){assertTrue(e.getCause() instanceof IOException);}
        }
    }
}

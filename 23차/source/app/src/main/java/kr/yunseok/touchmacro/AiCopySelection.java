package kr.yunseok.touchmacro;

/** 실제 예측 시작점으로 공통 전체 구간 커널의 끝 후보를 결정합니다. */
final class AiCopySelection {
    private AiCopySelection(){}
    static int argmax(float[] values,boolean[] allowed){if(allowed!=null&&allowed.length!=values.length)throw AiJson.error("모델 출력과 마스크 길이 불일치");int chosen=-1;float best=-Float.MAX_VALUE;for(int i=0;i<values.length;i++){if(!Float.isFinite(values[i]))throw AiJson.error("모델 출력의 비유한 수치");if((allowed==null||allowed[i])&&(chosen<0||values[i]>best)){chosen=i;best=values[i];}}if(chosen<0)throw AiJson.error("복사 가능한 원문 구간 없음");return chosen;}
    static int[] span(AiTokenizer.Encoded input,float[] starts,float[] ends){if(starts.length!=input.ids.length||ends.length!=input.ids.length)throw AiJson.error("복사 출력과 입력 길이 불일치");int begin=argmax(starts,input.startKeep),end=argmax(ends,input.endMask(begin));return input.tokensToSpan(begin,end);}
}

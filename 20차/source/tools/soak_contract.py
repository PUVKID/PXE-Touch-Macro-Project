"""시간을 이어 붙이지 않는 연속 시험 판정과 제한된 기록 도구입니다."""
import json
import os
from pathlib import Path


def atomic_json(path, value):
    atomic_bytes(path,(json.dumps(value,ensure_ascii=False,indent=2)+'\n').encode('utf-8'))


def atomic_bytes(path, value):
    path=Path(path);path.parent.mkdir(parents=True,exist_ok=True)
    temporary=path.with_name(path.name+'.tmp')
    with temporary.open('wb') as stream:
        stream.write(value)
        stream.flush();os.fsync(stream.fileno())
    os.replace(temporary,path)


class RotatingBytes:
    def __init__(self,path,limit=1048576,copies=4):
        self.path=Path(path);self.limit=limit;self.copies=copies
        self.path.parent.mkdir(parents=True,exist_ok=True)

    def write(self,data):
        for offset in range(0,len(data),self.limit):
            part=data[offset:offset+self.limit]
            if self.path.exists() and self.path.stat().st_size+len(part)>self.limit:
                oldest=Path(str(self.path)+'.'+str(self.copies))
                if oldest.exists():oldest.unlink()
                for index in range(self.copies-1,0,-1):
                    source=Path(str(self.path)+'.'+str(index))
                    if source.exists():os.replace(source,Path(str(self.path)+'.'+str(index+1)))
                os.replace(self.path,Path(str(self.path)+'.1'))
            with self.path.open('ab') as stream:stream.write(part)


def assess(previous,current,host_gap_seconds):
    def verdict(result,reason='',warning=False):
        return dict(result=result,reason=reason,warning=warning)
    integer_keys=('seq','pid','generation','elapsed_ms','uptime_ms','started_elapsed_ms',
                  'cycles','rounds','entries','backs','ocr_count','capture_ms',
                  'last_progress_elapsed_ms','requested_seconds')
    if not isinstance(current,dict) or any(type(current.get(key)) is not int or current[key]<0 for key in integer_keys):
        return verdict('FAIL','상태 정수 필드 오류')
    if not current.get('run_id') or not current.get('boot_id') or current['pid']==0:
        return verdict('FAIL','실행 식별 근거 없음')
    if current.get('errors') or current.get('result')=='FAIL':return verdict('FAIL','장치 시험 실패: '+str(current.get('errors','')))
    if host_gap_seconds>90:return verdict('INCONCLUSIVE','host 또는 장치 heartbeat 90초 초과')
    if previous:
        if any(current[key]!=previous[key] for key in ('run_id','pid','boot_id','generation','started_elapsed_ms','requested_seconds')):
            return verdict('FAIL','연속 실행 식별 변경')
        if current['seq']==previous['seq']:
            return verdict('RUNNING') if current==previous else verdict('FAIL','동일 순번의 상태 내용 변경')
        if any(current[key]<previous[key] for key in ('seq','elapsed_ms','uptime_ms','cycles','rounds','entries','backs','ocr_count','capture_ms')):
            return verdict('FAIL','시간 또는 실제 진행 역행')
        if current['elapsed_ms']-previous['elapsed_ms']-(current['uptime_ms']-previous['uptime_ms'])>2000:
            return verdict('INCONCLUSIVE','장치 elapsed와 uptime 사이 수면 공백')
    if 'started_uptime_ms' in current and current['elapsed_ms']-(current['uptime_ms']-current['started_uptime_ms'])>2000:
        return verdict('INCONCLUSIVE','장치 수면 시간 포함')
    if not(current['cycles']<=current['backs']<=current['cycles']+1 and current['backs']<=current['entries']<=current['backs']+1):
        return verdict('FAIL','진입·뒤로가기·완료 수 불일치')
    if not(current['backs']*15<=current['rounds']<=current['backs']*15+15):return verdict('FAIL','15회 반복 불일치')
    stalled=current['started_elapsed_ms']+current['elapsed_ms']-current['last_progress_elapsed_ms']
    if stalled>=300000:return verdict('FAIL','실제 진행 300초 정체')
    if current.get('result')=='PASS':
        if current['requested_seconds'] not in (300,86400) or current['elapsed_ms']<current['requested_seconds']*1000:
            return verdict('FAIL','요청한 연속 시간 미충족')
        if current['cycles']<3 or current['rounds']!=current['cycles']*15 or not(current['entries']==current['backs']==current['cycles']):
            return verdict('FAIL','최종 15회 완료 경계 불일치')
        if current['ocr_count']<current['rounds']*2 or current['capture_ms']<=0:return verdict('FAIL','실제 OCR·캡처 처리 근거 부족')
        return verdict('PASS')
    if current.get('result')!='RUNNING':return verdict('FAIL','지원하지 않는 장치 상태')
    return verdict('RUNNING','실제 진행 180초 정체 경고' if stalled>=180000 else '',stalled>=180000)


class HostProof:
    """고유 실행과 host의 실제 경과를 장치 주장과 독립 대조합니다."""
    def __init__(self,run_id,seconds,started):
        self.run_id=run_id;self.seconds=seconds;self.started=started
        self.previous=None;self.last_new=started;self.saw_running=False

    def accept(self,current,now,gap,sleep_ms=0):
        def stop(result,reason):return dict(result=result,reason=reason,warning=False)
        if current.get('run_id')!=self.run_id or current.get('requested_seconds')!=self.seconds:
            return stop('FAIL','예상 실행 ID 또는 시간 불일치')
        if any(type(current.get(key)) is not int for key in ('started_uptime_ms','observed_generation')):
            return stop('FAIL','필수 실제 장치 관측 없음')
        if sleep_ms>2000:return stop('INCONCLUSIVE','host 수면 또는 절전 시간 관측')
        if not self.saw_running:
            if current.get('result')!='RUNNING' or current.get('elapsed_ms',60001)>60000 or now-self.started>75:
                return stop('FAIL','초기 실제 RUNNING 관측 없음')
            self.saw_running=True
        if not self.previous or current.get('seq')!=self.previous.get('seq'):self.last_new=now
        result=assess(self.previous,current,max(gap,now-self.last_new))
        if result['result'] not in ('RUNNING','PASS'):return result
        if current['elapsed_ms']>(now-self.started)*1000+5000:return stop('FAIL','장치 경과가 host 실제 경과를 앞섬')
        if current.get('result')=='PASS':
            if now-self.started<self.seconds:return stop('FAIL','host 실제 연속 시간 미충족')
            if current.get('observed_generation')!=current['generation']+1 or current.get('running') is not False or not 0<current.get('stop_ms',0)<1000:
                return stop('FAIL','최종 실제 정지 관측 불일치')
        elif current['observed_generation']!=current['generation'] or current.get('running') is not True or current.get('watching') is not True or current.get('paused') is not False:
            return stop('FAIL','실제 실행 상태 또는 관측 세대 변경')
        self.previous=current
        return result

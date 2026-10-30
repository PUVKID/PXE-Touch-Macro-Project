from pathlib import Path
p=Path('app/src/main/java/kr/yunseok/touchmacro/Ui.java')
s=p.read_text(encoding='utf-8')
for a,b in {'#000000':'#101B24','#F2F2F7':'#FFFFFF','#1C1C1E':'#162630','#F5F5F7':'#F5FAFA','#1D1D1F':'#101B24','#A1A1AA':'#B4C5CD','#63636E':'#536675','#5EAAFF':'#56D7C7','#006BE6':'#007F78','#2C2C2E':'#203B42','#E9E9EF':'#E9F7F3'}.items():s=s.replace(a,b)
s=s.replace(' public static int dp(',''' public static int line(Context c){return Color.parseColor(dark(c)?"#31444E":"#DCE5E8");}
 public static GradientDrawable outline(Context c,int color,int radius){GradientDrawable g=rounded(color,dp(c,radius));g.setStroke(dp(c,1),line(c));return g;}
 // システム設定ではなく Android のアニメーター設定に従う短い入場遷移です。
 public static void enter(View v){if(!android.animation.ValueAnimator.areAnimatorsEnabled())return;v.setAlpha(0f);v.setTranslationY(dp(v.getContext(),6));v.animate().alpha(1f).translationY(0).setDuration(180).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();}
 public static int dp(''')
s=s.replace('// システム設定ではなく Android のアニメーター設定に従う短い入場遷移です。','// 시스템 애니메이션 설정을 따르며 동작 실행을 지연하지 않는 짧은 진입 전환입니다.')
s=s.replace('e.setBackground(rounded(soft(c),dp(c,13)))','e.setBackground(outline(c,soft(c),8))')
s=s.replace('b.setMinimumHeight(dp(c,44))','b.setMinimumHeight(dp(c,48))').replace('rounded(soft(c),dp(c,14))','outline(c,Color.TRANSPARENT,8)')
p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/kr/yunseok/touchmacro/MainActivity.java');s=p.read_text(encoding='utf-8')
s=s.replace('private LinearLayout page, list;private TextView state;','private LinearLayout page, list;private TextView state,dockProgress,dockTiming;')
s=s.replace('if(!testing&&shownConnected!=(MacroService.get()!=null))render();connectionUi.postDelayed(this,800);','if(!testing&&shownConnected!=(MacroService.get()!=null))render();if(!testing)updateDock();connectionUi.postDelayed(this,250);')
s=s.replace('@Override protected void onPause(){connectionUi.removeCallbacks(watchConnection);','@Override protected void onDestroy(){connectionUi.removeCallbacksAndMessages(null);if(page!=null)page.animate().cancel();super.onDestroy();}\n    @Override protected void onPause(){connectionUi.removeCallbacks(watchConnection);if(page!=null)page.animate().cancel();')
a=s.index('        LinearLayout c=new LinearLayout(this);',s.index('private LinearLayout card('));b=s.index('\n    private void content',a)
s=s[:a]+'''        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(14));c.setBackground(Ui.outline(this,Ui.card(this),8));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(4),0,dp(6));parent.addView(c,p);return c;
    }
'''+s[b:]
s=s.replace('testing=false;blue=Ui.accent(this);','testing=false;dockProgress=null;dockTiming=null;blue=Ui.accent(this);')
a=s.index('        LinearLayout brand=row(header)');b=s.index('        boolean connected=',a)
s=s[:a]+'''        LinearLayout brand=row(header);brand.setGravity(Gravity.CENTER_VERTICAL);TextView logo=text(brand,"PXE",25,ink);logo.setLetterSpacing(.02f);
        ImageView brandIcon=new ImageView(this);brandIcon.setImageResource(R.drawable.pxe_focus);brandIcon.setColorFilter(blue);LinearLayout.LayoutParams brandSize=new LinearLayout.LayoutParams(dp(25),dp(25));brandSize.setMargins(dp(7),0,dp(12),0);brand.addView(brandIcon,brandSize);View flexible=new View(this);brand.addView(flexible,new LinearLayout.LayoutParams(0,1,1));
'''+s[b:]
s=s.replace('connected?"연결됨":"연결하기",10,connected?muted:blue','connected?"● 연결됨":"연결하기",11,connected?blue:muted')
s=s.replace('shortcut.setTextSize(11);','shortcut.setTextSize(11);shortcut.setBackgroundColor(Color.TRANSPARENT);')
s=s.replace('page.setPadding(dp(24),dp(18),dp(24),dp(32))','page.setPadding(dp(24),dp(16),dp(24),dp(24))')
s=s.replace('text(page,"PXE 1.3.0 · 13차\\n작은 동작을, 편안하게.",11,muted);','text(page,"PXE 1.4.0 · 14차",11,muted);')
s=s.replace('text(page,"PXE를 나에게 맞게.",13,muted);','text(page,"화면과 실행 환경을 설정하세요.",13,muted);')
s=s.replace('space(page,8);LinearLayout resources=card(page);resources.setPadding(dp(8),dp(4),dp(8),dp(4));','space(page,16);text(page,"기록과 도움말",13,muted);LinearLayout resources=new LinearLayout(this);resources.setOrientation(LinearLayout.VERTICAL);page.addView(resources);')
s=s.replace('space(page,16);primary(page,"매크로 시작",this::start);','space(page,12);settingRow(page,"반복 · 시간 설정",(macro.rounds==0?"무한":macro.rounds+"회")+" 반복 · 지연 시간 설정",this::settings);space(page,16);primary(page,"매크로 시작",this::start);')
s=s.replace('        addNavigation(shell);content(shell);Ui.font(shell);','        addStatusDock(shell);addNavigation(shell);content(shell);Ui.font(shell);Ui.enter(page);')
a=s.index('    private void addNavigation(');b=s.index('    private void buildWatchPage()',a)
s=s[:a]+'''    private void addStatusDock(LinearLayout shell){
        if(tab!=4)return;
        LinearLayout dock=new LinearLayout(this);dock.setOrientation(LinearLayout.VERTICAL);dock.setPadding(dp(24),dp(10),dp(24),dp(10));dock.setBackgroundColor(Ui.soft(this));shell.addView(dock,new LinearLayout.LayoutParams(-1,-2));
        dockProgress=text(dock,"",13,ink);dockProgress.setPadding(0,0,0,dp(4));dockProgress.setMaxLines(2);dockProgress.setContentDescription("실행 상태");
        dockTiming=text(dock,"",10,muted);dockTiming.setPadding(0,0,0,0);dockTiming.setMaxLines(2);dockTiming.setContentDescription("실행 시간");updateDock();
    }
    private void updateDock(){
        if(dockProgress==null||dockTiming==null)return;MacroService svc=MacroService.get();
        String progress=svc==null?"대기 중 · 연결 필요":svc.progressText(),timing=svc==null?"접근성을 연결하면 실행 상태가 표시됩니다.":svc.timingText();
        if(!progress.contentEquals(dockProgress.getText()))dockProgress.setText(progress);
        if(!timing.contentEquals(dockTiming.getText()))dockTiming.setText(timing);
    }
    private void rule(LinearLayout parent){View line=new View(this);line.setBackgroundColor(Ui.line(this));parent.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));}
    private void addNavigation(LinearLayout shell){
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER_VERTICAL);nav.setPadding(dp(8),0,dp(8),0);nav.setBackgroundColor(Ui.bg(this));rule(shell);shell.addView(nav,new LinearLayout.LayoutParams(-1,dp(68)));
        String[] labels={"탐지","동작","글자","설정"};int[] pages={4,1,0,3},icons={R.drawable.pxe_scan,R.drawable.pxe_steps,R.drawable.pxe_text,R.drawable.pxe_settings};
        for(int i=0;i<pages.length;i++){final int chosen=pages[i];boolean active=tab==chosen||(chosen==1&&tab==2);LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setGravity(Gravity.CENTER);item.setContentDescription(labels[i]);item.setClickable(true);item.setBackground(Ui.ripple(this,Color.TRANSPARENT,0));nav.addView(item,new LinearLayout.LayoutParams(0,-1,1));
            ImageView icon=new ImageView(this);icon.setImageResource(icons[i]);icon.setColorFilter(active?blue:muted);icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);item.addView(icon,new LinearLayout.LayoutParams(dp(23),dp(23)));TextView label=text(item,labels[i],11,active?blue:muted);label.setPadding(0,dp(4),0,dp(5));label.setGravity(Gravity.CENTER);label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);View marker=new View(this);marker.setBackground(Ui.rounded(active?blue:Color.TRANSPARENT,dp(2)));item.addView(marker,new LinearLayout.LayoutParams(dp(44),dp(3)));item.setOnClickListener(v->{tab=chosen;render();});}
    }
    private void actionRow(LinearLayout parent,int icon,String title,String detail,boolean selected,Runnable action){
        LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(12),dp(14),dp(12),dp(14));r.setMinimumHeight(dp(76));r.setBackground(Ui.ripple(this,selected?Ui.soft(this):Color.TRANSPARENT,8));parent.addView(r,new LinearLayout.LayoutParams(-1,-2));
        ImageView symbol=new ImageView(this);symbol.setImageResource(icon);symbol.setColorFilter(selected?blue:muted);symbol.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);LinearLayout.LayoutParams iconSize=new LinearLayout.LayoutParams(dp(26),dp(26));iconSize.setMargins(0,0,dp(16),0);r.addView(symbol,iconSize);
        LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);r.addView(copy,new LinearLayout.LayoutParams(0,-2,1));TextView label=text(copy,title,16,ink);label.setPadding(0,0,0,dp(5));TextView value=text(copy,detail,12,muted);value.setPadding(0,0,0,0);
        ImageView arrow=new ImageView(this);arrow.setImageResource(R.drawable.pxe_chevron);arrow.setColorFilter(muted);arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);r.addView(arrow,new LinearLayout.LayoutParams(dp(20),dp(20)));r.setOnClickListener(v->action.run());r.setContentDescription(title+" · "+detail);if(!selected)rule(parent);
    }
'''+s[b:]
a=s.index('        text(page,"탐지",32,ink);',s.index('private void buildWatchPage'));b=s.index('        space(page,12);primary(page,"탐지 시작"',a)
s=s[:a]+'''        text(page,"찾을 글자",15,muted);LinearLayout heading=row(page);heading.setGravity(Gravity.CENTER_VERTICAL);TextView word=text(heading,p.rule.text,34,ink);word.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
        ImageView focus=new ImageView(this);focus.setImageResource(R.drawable.pxe_focus);focus.setColorFilter(blue);heading.addView(focus,new LinearLayout.LayoutParams(dp(44),dp(44)));space(page,6);View underline=new View(this);underline.setBackground(Ui.rounded(blue,dp(2)));page.addView(underline,new LinearLayout.LayoutParams(dp(46),dp(3)));space(page,24);
        actionRow(page,R.drawable.pxe_focus,"탐지 영역",p.areaCount()+"개 영역 · 순서대로 확인",true,this::watchRegion);space(page,8);
        actionRow(page,R.drawable.pxe_steps,"실행 매크로",(p.macroName.isEmpty()?Store.load(this).name:p.macroName)+" · "+p.repetitions+"회",false,this::watchSettings);
        actionRow(page,R.drawable.pxe_clock,"완료 후 대기",Times.durationLabel(p.cooldownMs),false,this::watchSettings);
        actionRow(page,R.drawable.pxe_refresh,"새로고침 간격",p.intervalSeconds+"초",false,this::watchSettings);
'''+s[b:]
s=s.replace('private Button primary(LinearLayout box,String label,Runnable action){Button b=button(box,label,action);b.setTextSize(15);','private Button primary(LinearLayout box,String label,Runnable action){Button b=button(box,label,action);b.setTextSize(17);').replace('b.setBackground(Ui.rounded(blue,dp(14)))','b.setBackground(Ui.ripple(this,blue,8))')
s=s.replace('b.setTextSize(12);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1);','b.setTextSize(12);b.setBackground(Ui.ripple(this,Color.TRANSPARENT,0));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1);')
a=s.index('    private void settingsAction(');b=s.index('    private void recognitionSettings()',a)
s=s[:a]+'''    private void settingsAction(LinearLayout box,String label,Runnable action){actionRow(box,label.equals("인식 테스트")?R.drawable.pxe_eye:R.drawable.pxe_text,label,"",false,action);}
    private void settingRow(LinearLayout box,String title,String value,Runnable action){actionRow(box,title.equals("접근성")?R.drawable.pxe_focus:title.equals("화면 모드")?R.drawable.pxe_sun:R.drawable.pxe_settings,title,value,false,action);}
'''+s[b:]
p.write_text(s,encoding='utf-8')
# 시안의 기본 벡터 표현을 앱 전체에서 재사용합니다.
d=Path('app/src/main/res/drawable')
paths={'pxe_focus':'M8,3H5Q3,3 3,5V8M16,3H19Q21,3 21,5V8M3,16V19Q3,21 5,21H8M21,16V19Q21,21 19,21H16','pxe_clock':'M12,3A9,9 0,1 0,12 21A9,9 0,1 0,12 3M12,7V12L16,14','pxe_refresh':'M20,8A9,9 0,0 0,5 5L3,8M3,3V8H8M4,16A9,9 0,0 0,19 19L21,16M16,16H21V21','pxe_chevron':'M9,5L16,12L9,19','pxe_eye':'M2,12Q12,-2 22,12Q12,26 2,12M15,12A3,3 0,1 0,9 12A3,3 0,1 0,15 12','pxe_sun':'M12,7A5,5 0,1 0,12 17A5,5 0,1 0,12 7M12,1V3M12,21V23M1,12H3M21,12H23M4,4L6,6M18,18L20,20M4,20L6,18M18,6L20,4'}
for name,path in paths.items():
 (d/(name+'.xml')).write_text(f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="@android:color/transparent" android:strokeColor="#101B24" android:strokeWidth="1.8" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="{path}"/></vector>\n',encoding='utf-8')
for f in [Path('app/src/main/res/values/styles.xml'),*d.glob('nex_dialog*.xml'),*d.glob('ic_*.xml'),*d.glob('pxe_*.xml')]:
 s=f.read_text(encoding='utf-8')
 for a,b in {'#F2F2F7':'#FFFFFF','#000000':'#101B24','#1C1C1E':'#162630','#161617':'#101B24','#5EAAFF':'#56D7C7','#006BE6':'#007F78','#1D1D1F':'#101B24'}.items():s=s.replace(a,b)
 f.write_text(s,encoding='utf-8')
for name in ['SetupCoach.java','RunHud.java']:
 p=Path('app/src/main/java/kr/yunseok/touchmacro')/name;s=p.read_text(encoding='utf-8');s=s.replace('Ui.rounded(Ui.card(c),Ui.dp(c,24))','Ui.outline(c,Ui.card(c),12)').replace('Ui.rounded(Ui.card(c),Ui.dp(c,22))','Ui.outline(c,Ui.card(c),10)').replace('Ui.dp(c,14)','Ui.dp(c,8)');p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/kr/yunseok/touchmacro/FloatingController.java');s=p.read_text(encoding='utf-8').replace('box.setBackground(Ui.rounded(Ui.card(c),scaled(24)))','box.setBackground(Ui.outline(c,Ui.card(c),14))').replace('box.setElevation(dp(4))','box.setElevation(dp(2))');p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/kr/yunseok/touchmacro/SetupWizard.java');s=p.read_text(encoding='utf-8').replace('Ui.dp(a,14)','Ui.dp(a,8)');p.write_text(s,encoding='utf-8')
p=Path('app/build.gradle');s=p.read_text(encoding='utf-8').replace('versionCode 13','versionCode 14').replace("versionName '1.3.0'","versionName '1.4.0'");p.write_text(s,encoding='utf-8')
p=Path('빌드.ps1');s=p.read_text(encoding='utf-8').replace('13','14').replace('v1.3','v1.4');p.write_text(s,encoding='utf-8')
p=Path('tools/check_artifacts.py');s=p.read_text(encoding='utf-8').replace('PXE13Build','PXE14Build').replace('12차_보존기준','13차_보존기준').replace('12차 원본','13차 원본').replace('PXE_13차_v1.3_','PXE_14차_v1.4_').replace("versionCode='13'","versionCode='14'").replace("versionName='1\\.3\\.0'","versionName='1\\.4\\.0'").replace('13/1.3.0','14/1.4.0');p.write_text(s,encoding='utf-8')
p=Path('tools/check_device.py');s=p.read_text(encoding='utf-8').replace('PXE13','PXE14').replace('13차','14차').replace('versionCode=13','versionCode=14');p.write_text(s,encoding='utf-8')
Path('빌드방법.txt').write_text('''PXE 1.4.0 · 14차 소스 빌드

source의 빌드.ps1은 D:/PXE14Build 전용 연결로 JDK 17 / Gradle 8.13 / Android SDK 35를 사용합니다.
기존 개인 서명 키와 앱 ID를 유지합니다. 키는 소스에 포함하지 않습니다.
휴대전화용 ARM64 빌드를 먼저 저장하고 PC용 ARM64/x86_64 APK와 기기 검사 APK를 빌드합니다.
납품 위치: D:/2026/PXE Project/14차

검사: :app:testDebugUnitTest :app:lintDebug
순수 모델: python -B -X utf8 tools/check_regression.py
텍스트: python -B -X utf8 tools/check_utf8.py
APK·13차 보존: python -B -X utf8 tools/check_artifacts.py
소유 PXE14 AVD에서만: python -B -X utf8 tools/check_device.py <검사 클래스>
기기 검사는 emulator-5598 / 전용 ADB 서버 5043을 사용합니다.
기존 모델·저장 형식과 내부 밀리초 단위를 유지합니다. 화면 입력은 분/초입니다.
''',encoding='utf-8')
print('UI/리소스/14차 경로 초기 구현 저장')

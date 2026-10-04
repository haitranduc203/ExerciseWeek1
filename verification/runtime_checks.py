from pathlib import Path
import subprocess as sp
import time, re, json

ROOT=Path(r'D:\Exercie')
ADB=r'C:\Users\ADMIN\AppData\Local\Android\Sdk\platform-tools\adb.exe'
CLI=r'C:\Users\ADMIN\AppData\AndroidCLI\android.exe'
NAMES=['Bai01_ActivityTasks','Bai02_Localization','Bai03_ForegroundService','Bai04_AIDL','Bai05_Gallery','Bai06_Broadcast','Bai07_FullScreenIntent']
def cmd(*args):
    p=sp.run([str(x) for x in args],capture_output=True,encoding='utf-8',errors='replace',timeout=60)
    if p.returncode: raise RuntimeError(p.stdout+p.stderr)
    return p.stdout
def adb(*args): return cmd(ADB,*args)
def nodes():
    path=Path(__file__).with_name('current_layout.json')
    cmd(CLI,'layout','--flat','--pretty','--output='+str(path))
    data=json.loads(path.read_text(encoding='utf-8'))
    result=[]
    def visit(value):
        if isinstance(value,list):
            for item in value: visit(item)
        elif isinstance(value,dict):
            if 'bounds' in value:
                item=dict(value); item['clickable']='true' if 'CLICKABLE' in value.get('interactions',[]) else 'false'; result.append(item)
            for key in ['children','content']: visit(value.get(key,[]))
    visit(data); return result
def screen_text(): return '\n'.join(n.get('text','') for n in nodes())
def click(label):
    for attempt in range(4):
        for node in nodes():
            if label.casefold() in node.get('text','').casefold() and node.get('clickable')=='true':
                coords=list(map(int,re.findall(r'\d+',node.get('bounds'))))
                adb('shell','input','tap',str((coords[0]+coords[2])//2),str((coords[1]+coords[3])//2)); time.sleep(.8); return
        adb('shell','input','swipe','600','1800','600','650','400'); time.sleep(.4)
    raise AssertionError('Button not found: '+label)
def start(pkg):
    adb('shell','am','start','-W','-a','android.intent.action.MAIN','-c','android.intent.category.LAUNCHER','-f','0x10200000','-n',pkg+'/'+pkg+'.MainActivity'); time.sleep(.8)
def check(condition,label):
    if not condition: raise AssertionError(label)
    print('PASS '+label,flush=True)
def capture(evidence,name):
    nodes()
    (evidence/(name+'.json')).write_text(Path(__file__).with_name('current_layout.json').read_text(encoding='utf-8'),encoding='utf-8')
    cmd(CLI,'screen','capture','--device=emulator-5554','--output='+str(evidence/(name+'.png')))
def run(index):
    root=ROOT/NAMES[index-1]; evidence=root/'evidence'; evidence.mkdir(exist_ok=True)
    pkg=f'vn.training.bai{index:02}'
    if index==4:
        adb('shell','am','force-stop','vn.training.books.server')
        cmd(CLI,'run','--apks='+str(root/'server/build/outputs/apk/debug/server-debug.apk'),'--activity=vn.training.books.server.MainActivity','--device=emulator-5554')
        pkg='vn.training.books.client'; apk=root/'client/build/outputs/apk/debug/client-debug.apk'
    else: apk=root/'app/build/outputs/apk/debug/app-debug.apk'
    adb('shell','am','force-stop',pkg)
    if index==5:
        adb('shell','pm','revoke',pkg,'android.permission.READ_MEDIA_IMAGES')
        adb('shell','pm','revoke',pkg,'android.permission.READ_MEDIA_VISUAL_USER_SELECTED')
    adb('logcat','-c')
    deployment=cmd(CLI,'run','--apks='+str(apk),'--activity='+pkg+'.MainActivity','--device=emulator-5554')
    (evidence/'deploy.log').write_text(deployment,encoding='utf-8')
    time.sleep(1)
    initial=screen_text()
    check(pkg in adb('shell','dumpsys','activity','activities'),'launcher foreground')
    capture(evidence,'01_launcher')
    passed=['Cài APK và mở launcher không crash (Android 14/API34)']
    if index==1:
        click('Mở BStandardActivity'); click('Mở BStandardActivity')
        log=adb('logcat','-d','-s','StackDemo:I','*:S')
        check(len(re.findall(r'onCreate BStandardActivity',log))==2,'B standard creates two instances')
        adb('shell','am','force-stop',pkg); start(pkg)
        click('Mở BTopActivity'); click('Mở BTopActivity')
        log=adb('logcat','-d','-s','StackDemo:I','*:S')
        check('onNewIntent BTopActivity' in log,'singleTop reuses top instance')
        passed+=['B standard tạo hai instance; B singleTop gọi onNewIntent khi ở đỉnh']
    elif index==2:
        click('Tiếng Việt'); check('Xin chào' in screen_text(),'Vietnamese locale applied')
        adb('shell','am','force-stop',pkg); start(pkg)
        check('Xin chào' in screen_text(),'locale persists after force-stop')
        click('日本語'); check('こんにちは' in screen_text(),'Japanese locale applied')
        passed+=['Đổi Việt/Nhật và giữ locale sau force-stop']
    elif index==3:
        click('Play / Start'); time.sleep(3)
        check('playing=true' in screen_text(),'FGS playing')
        click('Pause'); time.sleep(1); check('playing=false' in screen_text(),'FGS paused')
        click('Stop'); time.sleep(1); check('Service=false' in screen_text(),'FGS stopped')
        log=adb('logcat','-d','-s','PlayerDemo:I','*:S')
        check('DefaultDispatcher-worker' in log and 'stopped' in log,'background thread and cleanup logged')
        passed+=['Play/Pause/Stop hoạt động, coroutine worker thread, Service dừng']
    elif index==4:
        check('connected=2' in screen_text(),'two AIDL sessions connected')
        click('Thêm sách từ cả 2 phiên'); time.sleep(1)
        check('Callback session 1' in screen_text() and 'Callback session 2' in screen_text(),'both sessions receive callbacks')
        click('Lấy danh sách'); check('count=2' in screen_text(),'remote count equals 2')
        adb('shell','am','force-stop','vn.training.books.server'); time.sleep(1)
        check('Mất kết nối' in screen_text(),'server death handled')
        click('Kết nối lại'); time.sleep(1); check('connected=2' in screen_text(),'rebind after server death')
        passed+=['Hai phiên bind, hai callback, count=2, xử lý server chết và reconnect']
    elif index==5:
        check('DENIED' in screen_text(),'denied permission handled')
        click('Tạo và lưu bitmap'); time.sleep(1)
        check('Saved: content://' in screen_text(),'bitmap published to MediaStore')
        click('Mô phỏng lỗi lưu'); time.sleep(1)
        check('Lưu thất bại, đã cleanup' in screen_text(),'failed write cleans pending entry')
        passed+=['DENIED không crash, lưu bitmap JPEG thành công, lỗi lưu mô phỏng cleanup']
    elif index==6:
        click('Gửi broadcast nội bộ'); check('Custom event #1' in screen_text(),'one event one callback')
        adb('shell','input','keyevent','3'); start(pkg)
        click('Gửi broadcast nội bộ'); check('Custom event #2' in screen_text(),'no duplicate after Home/reentry')
        passed+=['Custom broadcast mỗi lần đúng một callback, Home/quay lại không đăng ký trùng']
    elif index==7:
        adb('shell','pm','grant',pkg,'android.permission.POST_NOTIFICATIONS')
        adb('shell','am','force-stop',pkg); start(pkg)
        click('Gửi 2 sự kiện'); time.sleep(1)
        log=adb('logcat','-d','-s','FullScreenDemo:I','*:S')
        ids=re.findall(r'event=(\d+) label=Sự kiện',log)
        check(len(set(ids))==2,'two unique notification event IDs')
        passed+=['Hai notification có ID riêng; xem log FullScreenDemo']
    final=screen_text(); (evidence/'ui.txt').write_text(final,encoding='utf-8')
    capture(evidence,'02_after_actions')
    logs=adb('logcat','-d','-v','threadtime')
    relevant='\n'.join(line for line in logs.splitlines() if any(t in line for t in ['StackDemo','PlayerDemo','SyncDemo','BookServer','BookClient','BroadcastDemo','FullScreenDemo','FATAL EXCEPTION',pkg]))
    (evidence/'runtime.log').write_text(relevant,encoding='utf-8')
    check('FATAL EXCEPTION' not in relevant,'no runtime crash')
    with (root/'RESULTS.md').open('a',encoding='utf-8') as f:
        f.write('\n## Đã đo trên Pixel 7 Pro Android 14 (API34)\n'+'\n'.join('- '+p for p in passed)+'\n\nẢnh: evidence/01_launcher.png, 02_after_actions.png; log: runtime.log; layout: *.xml. Các kịch bản còn lại vẫn chưa kiểm chứng runtime.\n')
    print('FINISHED '+root.name,flush=True)

if __name__=='__main__':
    import sys
    for index in map(int,sys.argv[1:] or range(1,8)):
        try: run(index)
        except Exception as e:
            print(f'FAILED Bài {index}: {e}',flush=True)
            evidence=ROOT/NAMES[index-1]/'evidence'
            (evidence/'runtime_error.txt').write_text(str(e)+'\n'+adb('logcat','-d','-s','AndroidRuntime:E','*:S'),encoding='utf-8')

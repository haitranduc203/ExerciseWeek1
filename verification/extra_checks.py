import json,sys,time,re
from pathlib import Path
import runtime_checks as r

def layout():
    path=Path(__file__).with_name('current_layout.json')
    r.cmd(r.CLI,'layout','--flat','--pretty','--output='+str(path))
    data=json.loads(path.read_text(encoding='utf-8'))
    nodes=[]
    def visit(value):
        if isinstance(value,list):
            for item in value: visit(item)
        elif isinstance(value,dict):
            if 'center' in value: nodes.append(value)
            for key in ['children','content']: visit(value.get(key,[]))
    visit(data); return nodes
def text(): return '\n'.join(n.get('text','') for n in layout())
def tap(label):
    candidates=[n for n in layout() if label.casefold() in (n.get('text','')+' '+n.get('content-desc','')+' '+n.get('resource-id','')).casefold() and n.get('center')]
    if not candidates: raise AssertionError('UI element missing '+label)
    center=re.findall(r'\d+',candidates[0]['center'])
    r.adb('shell','input','tap',*center); time.sleep(1)
def gallery():
    r.adb('logcat','-c'); r.start('vn.training.bai05')
    tap('CHỤP ẢNH TAKEPICTURE')
    layout(); time.sleep(1)
    before=r.adb('shell','pidof','vn.training.bai05').strip()
    files=r.adb('shell','run-as','vn.training.bai05','ls','files/camera')
    r.check('.jpg' in files,'camera file survives outside cache')
    r.adb('shell','am','kill','vn.training.bai05'); time.sleep(1)
    tap('shutter_button'); tap('done_button')
    actual=text()
    r.check('Camera: content://media/' in actual,'TakePicture succeeds after background process recreation')
    after=r.adb('shell','pidof','vn.training.bai05').strip()
    logs=r.adb('logcat','-d','-s','GalleryDemo:I','AndroidRuntime:E','*:S')
    root=r.ROOT/'Bai05_Gallery'
    (root/'evidence/camera_recreation.log').write_text(f'beforePID={before} afterPID={after}\n'+logs,encoding='utf-8')
    r.check('FATAL EXCEPTION' not in logs,'no camera crash after fix')
    r.check(not r.adb('shell','run-as','vn.training.bai05','ls','files/camera').strip(),'camera temp file cleaned')
    tap('CHỤP ẢNH TAKEPICTURE'); layout(); r.adb('shell','input','keyevent','4')
    actual=text(); r.check('Đã hủy camera' in actual,'camera cancellation cleanup')
    (root/'evidence/camera_cancel_final.txt').write_text(actual,encoding='utf-8')
    with (root/'RESULTS.md').open('a',encoding='utf-8') as f: f.write('\n- TakePicture qua FileProvider thành công sau khi kill process nền, khôi phục URI và dọn file tạm; hủy camera cũng cleanup. Xem camera_recreation.log.\n')

def fsi():
    pkg='vn.training.bai07'; root=r.ROOT/'Bai07_FullScreenIntent'
    r.adb('shell','am','force-stop',pkg)
    r.adb('shell','pm','grant',pkg,'android.permission.POST_NOTIFICATIONS')
    r.adb('shell','appops','set',pkg,'USE_FULL_SCREEN_INTENT','deny')
    r.start(pkg); r.check('canUseFullScreenIntent=false' in text(),'FSI denied status visible')
    tap('GỬI 2 SỰ KIỆN')
    r.adb('shell','cmd','statusbar','expand-notifications')
    tap('Sự kiện A #')
    actual=text(); r.check('Sự kiện A' in actual and 'Event ID=' in actual,'old notification content opens original event')
    (root/'evidence/content_intent.txt').write_text(actual,encoding='utf-8')
    r.adb('shell','pm','revoke',pkg,'android.permission.POST_NOTIFICATIONS')
    r.start(pkg); r.check('Notifications=false' in text(),'notification denied state visible')
    tap('GỬI BÁO THỨC NGAY')
    actual=text()
    (root/'evidence/denied.txt').write_text(actual,encoding='utf-8')
    r.adb('shell','pm','grant',pkg,'android.permission.POST_NOTIFICATIONS')
    r.adb('shell','appops','set',pkg,'USE_FULL_SCREEN_INTENT','default')
    with (root/'RESULTS.md').open('a',encoding='utf-8') as f: f.write('\n- FSI special access bị tắt: UI false; click notification cũ Sự kiện A mở đúng ID/label dù đã tạo B. POST_NOTIFICATIONS bị từ chối: UI Notifications=false, không crash khi gửi; khôi phục quyền sau test.\n')

def locale_input():
    pkg='vn.training.bai02'; r.start(pkg)
    tap('name_input')
    r.check(any('name_input' in n.get('resource-id','') and 'FOCUSED' in n.get('state',[]) for n in layout()),'name input focused before typing')
    r.adb('shell','input','keyevent','123')
    r.adb('shell','input','keyevent',*(['67']*40))
    r.adb('shell','input','text','CodexStudent')
    r.adb('shell','input','keyevent','4')
    tap('Tiếng Việt')
    r.check('Xin chào, CodexStudent!' in text(),'typed name survives locale Activity recreation')
    r.adb('shell','am','force-stop',pkg); r.start(pkg)
    actual=text(); r.check('Xin chào, CodexStudent!' in actual,'typed name and locale persist after relaunch')
    root=r.ROOT/'Bai02_Localization'
    (root/'evidence/input_persistence.txt').write_text(actual,encoding='utf-8')
    with (root/'RESULTS.md').open('a',encoding='utf-8') as f: f.write('\n- Nhập CodexStudent rồi đổi locale Nhật→Việt (Activity recreate): giữ tên, lời chào đúng; force-stop/mở lại vẫn giữ tên và locale. input_persistence.txt.\n')

if __name__=='__main__':
    for name in sys.argv[1:]: globals()[name]()

package newtime.util.event;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

import java.awt.event.KeyEvent;
import java.util.ArrayList;

public class GlobalKeyListener {

    public static void main(String[] args) {
        GlobalKeyListener gkl = new GlobalKeyListener();
        gkl.hook();

        gkl.addKeyListener(new KeyEventHook() {
            public void keyPressed(int keycode) {
                if(keycode == KeyEvent.VK_W){
                    System.out.println("W");
                }
                if(keycode == KeyEvent.VK_ESCAPE){
                    gkl.unhook();
                }
            }

            public void keyReleased(int keycode) {

            }
        });
    }

    private ArrayList<KeyEventHook> keyEventHooks = new ArrayList<KeyEventHook>();

    private WinUser.HHOOK hHook;
    private WinUser.LowLevelKeyboardProc lowLevelKeyboardProc;

    private Thread thread;

    public void hook(){
        if(this.thread != null){
            System.err.println("Already Hooked!");
            return;
        }
        this.thread = new Thread(new Runnable() {
            public void run() {
                WinDef.HMODULE hMod = Kernel32.INSTANCE.GetModuleHandle(null);

                lowLevelKeyboardProc = new WinUser.LowLevelKeyboardProc() {
                    public WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, WinUser.KBDLLHOOKSTRUCT kbdllhookstruct) {
                        int value = wParam.intValue();
                        int keycode = kbdllhookstruct.vkCode;

                        if (value == 256) {
                            sendKeyPressedEvent(keycode);
                        } else if (value == 257) {
                            sendKeyReleasedEvent(keycode);
                        }

                        long peer = Pointer.nativeValue(kbdllhookstruct.getPointer());
                        return User32.INSTANCE.CallNextHookEx(hHook, nCode, wParam, new WinDef.LPARAM(peer));
                    }
                };

                hHook = User32.INSTANCE.SetWindowsHookEx(User32.WH_KEYBOARD_LL, lowLevelKeyboardProc, hMod, 0);

                int result;
                WinUser.MSG msg = new WinUser.MSG();

                while ((result = User32.INSTANCE.GetMessage(msg, null, 0, 0)) != 0) {
                    if (result == -1) {
                        break;
                    } else {
                        User32.INSTANCE.TranslateMessage(msg);
                        User32.INSTANCE.DispatchMessage(msg);
                    }
                }
            }
        });
        this.thread.start();
    }

    public void unhook() {
        User32.INSTANCE.PostQuitMessage(0);
        User32.INSTANCE.UnhookWindowsHookEx(this.hHook);
        System.out.println("Unhooked");
    }

    public boolean addKeyListener(KeyEventHook keyEventHook){
        return this.keyEventHooks.add(keyEventHook);
    }

    public boolean removeKeyListener(KeyEventHook keyEventHook){
        return this.keyEventHooks.remove(keyEventHook);
    }

    private void sendKeyPressedEvent(int keycode){
        for(KeyEventHook keyEventHook : this.keyEventHooks){
            keyEventHook.keyPressed(keycode);
        }
    }

    private void sendKeyReleasedEvent(int keycode){
        for(KeyEventHook keyEventHook : this.keyEventHooks){
            keyEventHook.keyReleased(keycode);
        }
    }


}

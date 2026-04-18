#include "org_balinhui_fpaplayer_nativeapis_Global.h"
#include <Windows.h>

#pragma comment(lib, "User32.lib")

JNIEXPORT jint JNICALL Java_org_balinhui_fpaplayer_nativeapis_Global_messageOf
(JNIEnv *env, jclass clazz, jlong hwnd, jstring title, jstring msg, jlong type) {
    const jchar* wtitle = env->GetStringChars(title, JNI_FALSE);
    const jchar* wmsg = env->GetStringChars(msg, JNI_FALSE);

    HWND window;
    if (hwnd == 0) {
        window = NULL;
    } else {
        window = (HWND) hwnd;
    }
    int result = MessageBoxW(
        window,
        reinterpret_cast<LPCWSTR>(wmsg), 
        reinterpret_cast<LPCWSTR>(wtitle), 
        type
    );
    
    env->ReleaseStringChars(title, wtitle);
    env->ReleaseStringChars(msg, wmsg);
    
    return result;
}

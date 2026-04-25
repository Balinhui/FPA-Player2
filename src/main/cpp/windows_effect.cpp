#include "org_balinhui_fpaplayer_nativeapis_Win32.h"
#include <Windows.h>
#include <dwmapi.h>

#pragma comment(lib, "dwmapi.lib")

JNIEXPORT jboolean JNICALL Java_org_balinhui_fpaplayer_nativeapis_Win32_applyWindowsEffect
(JNIEnv *env, jclass clazz, jlong lhwnd, jint type, jint attribute) {
    HWND hwnd = (HWND) lhwnd;

    MARGINS margins = {-1, -1, -1, -1};

    HRESULT hr = DwmExtendFrameIntoClientArea(hwnd, &margins);

    if (SUCCEEDED(hr)) {
        hr = DwmSetWindowAttribute(hwnd, type, &attribute, sizeof(attribute));
        if (SUCCEEDED(hr)) {
            return JNI_TRUE;
        }
    }

    return JNI_FALSE;
}

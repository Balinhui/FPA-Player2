#include "org_balinhui_fpaplayer_nativeapis_Global.h"
#include <Windows.h>

#pragma comment(lib, "User32.lib")

JNIEXPORT jlong JNICALL Java_org_balinhui_fpaplayer_nativeapis_Global_getHwnd
(JNIEnv *env, jclass clazz, jstring window_title) {
    const jchar *titleChars = env->GetStringChars(window_title, JNI_FALSE);
    jsize titleLength = env->GetStringLength(window_title);

    wchar_t* wTitle = (wchar_t*)malloc((titleLength + 1) * sizeof(wchar_t));
    if (wTitle == NULL) {
        return 0;
    }

    for (jsize i = 0; i < titleLength; i++) {
        wTitle[i] = (wchar_t)titleChars[i];
    }
    wTitle[titleLength] = L'\0';

    env->ReleaseStringChars(window_title, titleChars);

    HWND hwnd = FindWindowW(NULL, wTitle);

    free(wTitle);

    return reinterpret_cast<jlong>(hwnd);
}

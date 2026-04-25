#import <Cocoa/Cocoa.h>
#include "org_balinhui_fpaplayer_nativeapis_Global.h"
#include <dispatch/dispatch.h>

extern "C" {
JNIEXPORT jint JNICALL Java_org_balinhui_fpaplayer_nativeapis_Global_messageOf
  (JNIEnv *env, jclass clazz, jlong hwnd, jstring jtitle, jstring jmsg, jlong type) {
    const char *title = env->GetStringUTFChars(jtitle, JNI_FALSE);
    const char *msg = env->GetStringUTFChars(jmsg, JNI_FALSE);

    BOOL isMainThread = [NSThread isMainThread];

    if (isMainThread) {
        @autoreleasepool {
            NSAlert* alert = [[NSAlert alloc] init];
            [alert setMessageText:[NSString stringWithUTF8String:title]];
            [alert setInformativeText:[NSString stringWithUTF8String:msg]];
            [alert addButtonWithTitle:@"OK"];
            [alert runModal];
        }
    } else {
        dispatch_semaphore_t semaphore = dispatch_semaphore_create(0);
        dispatch_async(dispatch_get_main_queue(), ^{
            @autoreleasepool {
                NSAlert* alert = [[NSAlert alloc] init];
                [alert setMessageText:[NSString stringWithUTF8String:title]];
                [alert setInformativeText:[NSString stringWithUTF8String:msg]];
                [alert addButtonWithTitle:@"OK"];
                [alert runModal];
                dispatch_semaphore_signal(semaphore);
            }
        });
        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER);
    }
    
    env->ReleaseStringUTFChars(jtitle, title);
    env->ReleaseStringUTFChars(jmsg, msg);
    return 0;
}
}
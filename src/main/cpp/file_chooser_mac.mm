#import <Cocoa/Cocoa.h>
#include "org_balinhui_fpaplayer_nativeapis_Global.h"

JNIEXPORT jobjectArray JNICALL Java_org_balinhui_fpaplayer_nativeapis_Global_chooseFiles
  (JNIEnv *env, jclass, jlong, jobject) {
    __block NSArray *selectedPaths = nil;

    BOOL isMainThread = [NSThread isMainThread];

    if (isMainThread) {
        @autoreleasepool {
            NSOpenPanel *panel = [NSOpenPanel openPanel];

            [panel setCanChooseDirectories:NO];
            [panel setCanChooseFiles:YES];
            [panel setAllowsMultipleSelection:YES];
            [panel setCanCreateDirectories:NO];

            [panel setTitle:@"选择"];

            NSModalResponse response = [panel runModal];
            if (response == NSModalResponseOK) {
                // 获取所有选中的 URL
                NSArray<NSURL *> *urls = [panel URLs];
                if (urls && urls.count > 0) {
                    // 将 URL 数组转换为路径字符串数组
                    NSMutableArray *paths = [NSMutableArray arrayWithCapacity:urls.count];
                    for (NSURL *url in urls) {
                        [paths addObject:[url path]];
                    }
                    selectedPaths = [paths copy];
                }
            }
        }
    } else {
        dispatch_semaphore_t semaphore = dispatch_semaphore_create(0);
        dispatch_async(dispatch_get_main_queue(), ^{
            @autoreleasepool {
                NSOpenPanel *panel = [NSOpenPanel openPanel];

                [panel setCanChooseDirectories:NO];
                [panel setCanChooseFiles:YES];
                [panel setAllowsMultipleSelection:YES];
                [panel setCanCreateDirectories:NO];

                [panel setTitle:@"选择"];

                NSModalResponse response = [panel runModal];
                if (response == NSModalResponseOK) {
                    // 获取所有选中的 URL
                    NSArray<NSURL *> *urls = [panel URLs];
                    if (urls && urls.count > 0) {
                        // 将 URL 数组转换为路径字符串数组
                        NSMutableArray *paths = [NSMutableArray arrayWithCapacity:urls.count];
                        for (NSURL *url in urls) {
                            [paths addObject:[url path]];
                        }
                        selectedPaths = [paths copy];
                    }
                }
                dispatch_semaphore_signal(semaphore);
            }
        });
        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER);
    }

    //dispatch_release(semaphore);

    if (selectedPaths && selectedPaths.count > 0) {
        // 创建 Java String 数组
        jclass stringClass = env->FindClass("java/lang/String");
        if (stringClass == NULL) {
            return NULL;
        }
        
        jobjectArray result = env->NewObjectArray(selectedPaths.count, stringClass, NULL);
        if (result == NULL) {
            return NULL;
        }
        
        for (NSUInteger i = 0; i < selectedPaths.count; i++) {
            NSString *path = selectedPaths[i];
            jstring jPath = env->NewStringUTF([path UTF8String]);
            if (jPath != NULL) {
                env->SetObjectArrayElement(result, i, jPath);
                env->DeleteLocalRef(jPath);
            }
        }
        
        return result;
    }
    
    return NULL;
}

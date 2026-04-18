#include "org_balinhui_fpaplayer_nativeapis_Global.h"
#include <Windows.h>
#include <ShlObj.h>
#include <vector>
#include <string>

#pragma comment(lib, "User32.lib")
#pragma comment(lib, "Ole32.lib")

//用于将Java字符串转换为宽字符
std::wstring jstringToWstring(JNIEnv *env, jstring jstr) {
    if (jstr == nullptr) return L"";

    const jchar *chars = env->GetStringChars(jstr, JNI_FALSE);
    jsize length = env->GetStringLength(jstr);
    std::wstring wstr(reinterpret_cast<LPCWSTR>(chars), length);
    env->ReleaseStringChars(jstr, chars);
    return wstr;
}

//用于将宽字符转换为Java字符串
jstring wstringToJstring(JNIEnv *env, const PWSTR wstr) {
    return env->NewString(reinterpret_cast<const jchar *>(wstr), wcslen(wstr));
}

//从Java List<String>获取扩展名
std::vector<std::wstring> getSuffixNames(JNIEnv *env, jobject suffixNames) {
    std::vector<std::wstring> result;

    if (suffixNames == nullptr) return result;

    jclass listClass = env->GetObjectClass(suffixNames);
    jmethodID sizeMethod = env->GetMethodID(listClass, "size", "()I");
    jmethodID getMethod = env->GetMethodID(listClass, "get", "(I)Ljava/lang/Object;");

    jint size = env->CallIntMethod(suffixNames, sizeMethod);

    for (int i = 0; i < size; i++) {
        jobject element = env->CallObjectMethod(suffixNames, getMethod, i);
        jstring jstr = (jstring) element;

        if (jstr != nullptr) {
            result.push_back(jstringToWstring(env, jstr));
            env->DeleteLocalRef(element);
        }
    }

    env->DeleteLocalRef(listClass);
    return result;
}

//设置文件过滤器
std::vector<COMDLG_FILTERSPEC> setFilters(std::vector<std::wstring> suffixes,
    std::vector<wchar_t *> *allocatedStrings) {
    //创建文件类型过滤器
    std::vector<COMDLG_FILTERSPEC> filters;

    if (suffixes.empty()) {
        goto DONT_HAVE_EXT;
    }

    if (suffixes.size() > 1) {
        std::wstring allTypesName = L"推荐文件";
        std::wstring allTypesSpec;

        for (size_t i = 0; i < suffixes.size(); i++) {
            std::wstring ext = suffixes[i];
            if (i > 0) {
                allTypesSpec += L";";
            }
            allTypesSpec += L"*" + ext;
        }

        COMDLG_FILTERSPEC allFilter;
        wchar_t* pszName = new wchar_t[allTypesName.length() + 1];
        wcscpy_s(pszName, allTypesName.length() + 1, allTypesName.c_str());
        wchar_t* pszSpec = new wchar_t[allTypesSpec.length() + 1];
        wcscpy_s(pszSpec, allTypesSpec.length() + 1, allTypesSpec.c_str());
        allFilter.pszName = pszName;
        allFilter.pszSpec = pszSpec;

        filters.push_back(allFilter);
        allocatedStrings->push_back(pszName);
        allocatedStrings->push_back(pszSpec);
    }

    //为每个扩展名创建单独的过滤器
    for (const auto& suffix : suffixes) {
        std::wstring ext = suffix;
        std::wstring filterName = ext.substr(1) + L"文件 (*" + ext + L")";
        std::wstring filterSpec = L"*" + ext;

        COMDLG_FILTERSPEC filter;
        //需要确保字符串在filter使用期间一直有效
        //这里使用动态分配，注意要释放
        wchar_t* pszName = new wchar_t[filterName.length() + 1];
        wcscpy_s(pszName, filterName.length() + 1, filterName.c_str());
        wchar_t* pszSpec = new wchar_t[filterSpec.length() + 1];
        wcscpy_s(pszSpec, filterSpec.length() + 1, filterSpec.c_str());

        filter.pszName = pszName;
        filter.pszSpec = pszSpec;
        filters.push_back(filter);
        allocatedStrings->push_back(pszName);
        allocatedStrings->push_back(pszSpec);
    }

//如果没有扩展名，就直接添加所有文件
DONT_HAVE_EXT:

    //添加"所有文件"过滤器
    COMDLG_FILTERSPEC allFilesFilter;
    allFilesFilter.pszName = L"所有文件";
    allFilesFilter.pszSpec = L"*.*";
    filters.push_back(allFilesFilter);

    return filters;
}

JNIEXPORT jobjectArray JNICALL Java_org_balinhui_fpaplayer_nativeapis_Global_chooseFiles
(JNIEnv *env, jclass clazz, jlong hwnd, jobject suffixNames) {
    HRESULT hr = CoInitializeEx(NULL, COINIT_APARTMENTTHREADED | COINIT_DISABLE_OLE1DDE);
    if (FAILED(hr)) return nullptr;

    jobjectArray resultArray = nullptr;
    IFileOpenDialog *pFileOpen = NULL;

    try {
        //创建FileOpenDialog对象
        hr = CoCreateInstance(
            CLSID_FileOpenDialog,
            NULL,
            CLSCTX_ALL,
            IID_IFileOpenDialog,
            reinterpret_cast<void **>(&pFileOpen)
        );
        if (SUCCEEDED(hr)) {
            DWORD dwOptions;
            hr = pFileOpen->GetOptions(&dwOptions);
            if (SUCCEEDED(hr)) {
                hr = pFileOpen->SetOptions(
                    dwOptions | 
                    FOS_ALLOWMULTISELECT |
                    FOS_FILEMUSTEXIST|
                    FOS_FORCEFILESYSTEM
                );
            }

            //获取窗口句柄
            HWND hWndParent;
            if (hwnd == 0) {
                hWndParent = NULL;
            } else {
                hWndParent = (HWND) hwnd;
            }
            
            //获取扩展名列表
            std::vector<std::wstring> suffixes = getSuffixNames(env, suffixNames);
            
            //记录分配的字符串
            std::vector<wchar_t *> allocatedStrings;

            //创建文件类型过滤器
            std::vector<COMDLG_FILTERSPEC> filters = setFilters(suffixes, &allocatedStrings);
            if (!filters.empty()) {
                pFileOpen->SetFileTypes(filters.size(), filters.data());
                pFileOpen->SetFileTypeIndex(0); // 默认选中第一个过滤器
            }

            //设置标题
            pFileOpen->SetTitle(L"选择文件");

            //显示对话框
            hr = pFileOpen->Show(hWndParent);

            if (SUCCEEDED(hr)) {
                //获取用户选择的文件
                IShellItemArray *pItems = NULL;
                hr = pFileOpen->GetResults(&pItems);

                if (SUCCEEDED(hr)) {
                    DWORD count = 0;
                    pItems->GetCount(&count);

                    //创建Java字符串数组
                    jclass stringClass = env->FindClass("java/lang/String");
                    resultArray = env->NewObjectArray(count, stringClass, nullptr);

                    //遍历所有选中的文件
                    for (DWORD i = 0; i < count; i++) {
                        IShellItem *pItem = NULL;
                        hr = pItems->GetItemAt(i, &pItem);

                        if (SUCCEEDED(hr)) {
                            PWSTR pszFilePath = NULL;
                            hr = pItem->GetDisplayName(SIGDN_FILESYSPATH, &pszFilePath);

                            if (SUCCEEDED(hr)) {
                                jstring jstr = wstringToJstring(env, pszFilePath);
                                env->SetObjectArrayElement(resultArray, i, jstr);
                                env->DeleteLocalRef(jstr);

                                CoTaskMemFree(pszFilePath);
                            }
                            pItem->Release();
                        }
                    }
                    pItems->Release();
                }
            }

            //释放COM组件
            pFileOpen->Release();
            //释放之前动态分配的字符串
            if (!allocatedStrings.empty()) {
                for (wchar_t *str : allocatedStrings) {
                    delete[] str;
                }
                allocatedStrings.clear();
            }
        }
    } catch(...) {
        if (resultArray != nullptr) {
            env->DeleteLocalRef(resultArray);
            resultArray = nullptr;
        }
    }

    CoUninitialize();
    return resultArray;
}

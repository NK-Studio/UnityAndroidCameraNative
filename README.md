# Unity6 안드로이드 카메라 네이티브 렌더링 샘플
Q : 유니티의 WebcamTexture가 있는데 왜 이런 구현을 하나요?
A : 카메라에 대한 줌이나, 카메라를 통한 장난질을 하고 싶을 때 이런 샘플이 있으면 편할 듯 해서 만들어봤습니다.

# Support
- Unity6 or Higher Support
- OpenGL3 Support (Vulkan R&D 지원 예정)
- UnityActivity Only (UnityGameActivity R&D 중)

# Unity3D의 Texture2D로 Android 네이티브 카메라 이미지 표시하기

## 1. 구현 원리에 대한 개인적인 이해:
#### 먼저 C#에서 Texture2D를 생성하고 GetNativeTexturePtr()를 통해 포인터를 C++ 레이어에 전달합니다. C# 코드:

```
Texture2D tex = new Texture2D(1280, 720, TextureFormat.RGBA32, false);
// 픽셀을 선명하게 보기 위해 Point 필터링 설정
tex.filterMode = FilterMode.Point;
// GPU에 실제로 업로드하기 위해 Apply() 호출
tex.Apply();
//displayMaterial.mainTexture = tex;
rawImage.texture = tex;
SetTextureFromUnity(tex.GetNativeTexturePtr());
EnablePreview(true);
```

#### SetTextureFromUnity는 C++ 측에서 C#에서 호출할 수 있도록 선언된 네이티브 메서드입니다. C# 코드 조각:

```
[DllImport("NativeCameraPlugin")]
private static extern void SetTextureFromUnity(IntPtr texture);
```

#### SetTextureFromUnity의 C++ 코드:

```
extern "C" void UNITY_INTERFACE_EXPORT UNITY_INTERFACE_API SetTextureFromUnity(void *texturePtr)
{
    // 스크립트가 초기화 시점에 이것을 호출합니다; 여기서는 텍스처 포인터만 기억합니다.
    // 플러그인 렌더링 이벤트에서 매 프레임마다 텍스처 픽셀을 업데이트합니다
    // (텍스처 업데이트는 렌더링 스레드에서 발생해야 합니다).
    g_TexturePointer = texturePtr;
    LOGD("########################## SetTextureFromUnity texturePtr=%p\n", g_TexturePointer);
}
```

#### 이렇게 C++ 레이어는 C# 레이어의 Texture2D 포인터를 얻어 2D 텍스처를 계속 조작할 수 있습니다.
이제 2D 텍스처에 내용을 표시하기 위해서는 Unity 측에서 계속 GL.IssuePluginEvent(GetRenderEventFunc(), 1);를 호출하여 Android 네이티브 카메라의 이미지를 실시간으로 가져와 텍스처에 표시해야 합니다.
GetRenderEventFunc 함수는 C++의 네이티브 메서드입니다. 따라서 C#에서는 해당 라이브러리를 import 해야 합니다. 코드 조각:

```
extern "C" UnityRenderingEvent UNITY_INTERFACE_EXPORT UNITY_INTERFACE_API GetRenderEventFunc()
{
    return OnRenderEvent;
}
```

#### 위 코드는 OnRenderEvent 함수를 반환하며, OnRenderEvent의 주요 코드는 다음과 같습니다:

```
static void UNITY_INTERFACE_API OnRenderEvent(int eventID)
{
    if (g_TexturePointer)
    {
        int status;
        JNIEnv *env;
        int isAttached = 0;
        if (!gCallbackObject)
            return;
        if ((status = gJavaVM->GetEnv((void **)&env, JNI_VERSION_1_6)) < 0)
        {
            if ((status = gJavaVM->AttachCurrentThread(&env, NULL)) < 0)
            {
                return;
            }
            isAttached = 1;
        }
        jclass cls = env->GetObjectClass(gCallbackObject);
        if (!cls)
        {
            if (isAttached)
                gJavaVM->DetachCurrentThread();
            return;
        }
        jmethodID method = env->GetMethodID(cls, "requestJavaRendering", "(I)V");
        if (!method)
        {
            if (isAttached)
                gJavaVM->DetachCurrentThread();
            return;
        }
        GLuint gltex = (GLuint)(size_t)(g_TexturePointer);
        env->CallVoidMethod(gCallbackObject, method, (int)gltex);
        if (isAttached)
            gJavaVM->DetachCurrentThread();
    }
}
```

#### 이 함수의 기본 원리를 자세히 보면 JNI를 통해 Android Java 메서드인 requestJavaRendering을 호출합니다. requestJavaRendering 코드를 살펴보겠습니다:

```
private void requestJavaRendering(int texturePointer) {
        if (this._update) {
            int[] imageBuffer = new int[0];
            if (this._conversionScript != null) {
                imageBuffer = this._conversionScript.getOutputBuffer();
            }
            if (imageBuffer.length > 1) {
                GLES30.glBindTexture(GL_TEXTURE_2D, texturePointer);
                GLES30.glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, this._previewSize.getWidth(), this._previewSize.getHeight(), GL_RGBA, GL_UNSIGNED_BYTE, IntBuffer.wrap(imageBuffer));
            }
        }
    }
```

#### Java 메서드 requestJavaRendering의 핵심은 _conversionScript를 통해 imageBuffer 배열을 얻는 것입니다. 이 int 배열의 구체적인 값이 픽셀값이며, 비어있지 않다면 OpenGL을 통해 해당 텍스처에 업데이트됩니다. 핵심 코드는 GLES20.glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, this._previewSize.getWidth(), this._previewSize.getHeight(), GL_RGBA, GL_UNSIGNED_BYTE, IntBuffer.wrap(imageBuffer));입니다.
이제 this._conversionScript가 어디서 오는지 살펴보겠습니다. 생성 코드는 다음과 같습니다:

```
this._conversionScript = new YuvToRgb(this._renderScript, this._previewSize, CONVERSION_FRAME_RATE);
this._conversionScript.setOutputSurface(this._imagePreviewReader.getSurface());
this._previewSurface = this._conversionScript.getInputSurface();
```

#### Android Camera2 기반
C#에서 Android Camera2의 프레임 데이터를 표시하는 주요 기능이 구현됩니다.   
요즘은 Camera API가 CameraX까지 나왔습니다. 그래서 추후에 X로 구현을 다시 해볼 겁니다.
#### CMakeLists.txt 코드:

```
cmake_minimum_required(VERSION 3.4.1)

add_library( 
             NativeCameraPlugin
             SHARED
             RenderingPlugin.cpp )

find_library( # Sets the name of the path variable.
              dl
              GLESv3
              log )

target_link_libraries(
                       NativeCameraPlugin
                       log
                       ${log-lib} )
```

## 주의사항
Unity의 Texture2D는 Android Camera2의 미리보기 크기와 일치해야 합니다. (여기서는 모두 1280*720입니다.)  

또 하나 매우 중요한 점은 위의 CMakeLists 코드의 find_library가 GLESv3로 설정되어 있으므로, Unity 프로젝트도 OpenGL3로 설정해야 합니다!
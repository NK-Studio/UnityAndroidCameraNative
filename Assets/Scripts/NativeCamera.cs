using System;
using System.Collections;
using System.Runtime.InteropServices;
using UnityEngine;
using UnityEngine.Serialization;

public class NativeCamera : MonoBehaviour
{

    [DllImport("Camera-Rendering")]
    private static extern void SetTextureFromUnity(IntPtr texture);

    [DllImport("Camera-Rendering")]
    private static extern IntPtr GetRenderEventFunc();

    private AndroidJavaObject _androidJavaPlugin;

    public Material BackgroundMaterial;

    private void Start()
    {
        _androidJavaPlugin = new AndroidJavaObject("gmy.camera.CameraPluginActivity");

        CreateTextureAndPassToPlugin();
        StartCoroutine(CallPluginAtEndOfFrames());
        
        StartCamera();
    }

    /// <summary>
    /// 카메라를 실행합니다.
    /// </summary>
    public void StartCamera()
    {
        _androidJavaPlugin.Call("commonInit");
    }

    private void CreateTextureAndPassToPlugin()
    {
        Texture2D backgroundTexture = new Texture2D(1280, 720, TextureFormat.RGBA32, false);
        backgroundTexture.filterMode = FilterMode.Point;

        backgroundTexture.Apply();

        SetTextureFromUnity(backgroundTexture.GetNativeTexturePtr());
        BackgroundMaterial.mainTexture = backgroundTexture;
        EnablePreview(true);
    }
    
    private IEnumerator CallPluginAtEndOfFrames()
    {
        while (true)
        {
 
            yield return new WaitForEndOfFrame();
            GL.IssuePluginEvent(GetRenderEventFunc(), 1);
            yield return new WaitForEndOfFrame();
        }
    }

    public void EnablePreview(bool enable)
    {
        if (_androidJavaPlugin != null)
        {
            _androidJavaPlugin.Call("enablePreviewUpdater", enable);
        }
    }

    private void OnDestroy()
    {
        if (_androidJavaPlugin != null)
        {
            _androidJavaPlugin.Call("onRelease");
            _androidJavaPlugin.Dispose();
        }
    }
}
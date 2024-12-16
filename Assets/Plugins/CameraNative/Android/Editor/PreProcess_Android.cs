using UnityEditor.Android;


public class PreProcess_Android : AndroidProjectFilesModifier
{
    // Source files
    private const string RenderingPluginSourceHeader = "ExtraAndroidCode/RenderingPlugin.h";
    private const string RenderingPluginSourceCPP = "ExtraAndroidCode/RenderingPlugin.cpp";
    private const string RenderingPluginSourceCMake = "ExtraAndroidCode/CMakeLists.txt";
    
    private const string RenderingPluginSourceIUnityGraphics = "ExtraAndroidCode/Unity/IUnityGraphics.h";
    private const string RenderingPluginSourceIUnityInterface = "ExtraAndroidCode/Unity/IUnityInterface.h";
    private const string RenderingPluginSourceIUnityGraphicsD3D12 = "ExtraAndroidCode/Unity/IUnityGraphicsD3D12.h";
    private const string RenderingPluginSourceIUnityGraphicsD3D11 = "ExtraAndroidCode/Unity/IUnityGraphicsD3D11.h";
    private const string RenderingPluginSourceIUnityGraphicsD3D09 = "ExtraAndroidCode/Unity/IUnityGraphicsD3D9.h";
    
    // Destination files
    private const string RenderingPluginDestinationHeader = "unityLibrary/src/main/cpp/RenderingPlugin.h";
    private const string RenderingPluginDestinationCPP = "unityLibrary/src/main/cpp/RenderingPlugin.cpp";
    private const string RenderingPluginDestinationCMake = "unityLibrary/src/main/cpp/CMakeLists.txt";
    
    private const string RenderingPluginDestinationIUnityGraphics = "unityLibrary/src/main/cpp/Unity/IUnityGraphics.h";
    private const string RenderingPluginDestinationIUnityInterface = "unityLibrary/src/main/cpp/Unity/IUnityInterface.h";
    private const string RenderingPluginDestinationIUnityGraphicsD3D12 = "unityLibrary/src/main/cpp/Unity/IUnityGraphicsD3D12.h";
    private const string RenderingPluginDestinationIUnityGraphicsD3D11 = "unityLibrary/src/main/cpp/Unity/IUnityGraphicsD3D11.h";
    private const string RenderingPluginDestinationIUnityGraphicsD3D09 = "unityLibrary/src/main/cpp/Unity/IUnityGraphicsD3D9.h";

    public override AndroidProjectFilesModifierContext Setup()
    {
        var ctx = new AndroidProjectFilesModifierContext();
        ctx.Dependencies.DependencyFiles = new[]
        {
            RenderingPluginSourceHeader, RenderingPluginSourceCPP, RenderingPluginSourceCMake, RenderingPluginSourceIUnityGraphics,
            RenderingPluginSourceIUnityInterface, RenderingPluginSourceIUnityGraphicsD3D12, RenderingPluginSourceIUnityGraphicsD3D11, RenderingPluginSourceIUnityGraphicsD3D09
        };
     
        ctx.AddFileToCopy(RenderingPluginSourceHeader, RenderingPluginDestinationHeader);
        ctx.AddFileToCopy(RenderingPluginSourceCPP, RenderingPluginDestinationCPP);
        ctx.AddFileToCopy(RenderingPluginSourceCMake, RenderingPluginDestinationCMake);
        ctx.AddFileToCopy(RenderingPluginSourceIUnityGraphics, RenderingPluginDestinationIUnityGraphics);
        ctx.AddFileToCopy(RenderingPluginSourceIUnityInterface, RenderingPluginDestinationIUnityInterface);
        ctx.AddFileToCopy(RenderingPluginSourceIUnityGraphicsD3D12, RenderingPluginDestinationIUnityGraphicsD3D12);
        ctx.AddFileToCopy(RenderingPluginSourceIUnityGraphicsD3D11, RenderingPluginDestinationIUnityGraphicsD3D11);
        ctx.AddFileToCopy(RenderingPluginSourceIUnityGraphicsD3D09, RenderingPluginDestinationIUnityGraphicsD3D09);

        return ctx;
    }

    public override void OnModifyAndroidProjectFiles(AndroidProjectFiles projectFiles)
    {
    }
}

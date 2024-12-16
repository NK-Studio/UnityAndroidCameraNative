using UnityEditor.Android;


public class PreProcess_Android : AndroidProjectFilesModifier
{
    private const string RenderingPluginSourceHeader = "ExtraAndroidCode/RenderingPlugin.h";
    private const string RenderingPluginSourceCPP = "ExtraAndroidCode/RenderingPlugin.cpp";
    private const string RenderingPluginSourceCMake = "ExtraAndroidCode/CMakeLists.txt";

    private const string RenderingPluginDestinationHeader = "unityLibrary/src/main/cpp/RenderingPlugin.h";
    private const string RenderingPluginDestinationCPP = "unityLibrary/src/main/cpp/RenderingPlugin.cpp";
    private const string RenderingPluginDestinationCMake = "unityLibrary/src/main/cpp/CMakeLists.txt";

    public override AndroidProjectFilesModifierContext Setup()
    {
        var ctx = new AndroidProjectFilesModifierContext();
        ctx.Dependencies.DependencyFiles = new[]
        {
            RenderingPluginSourceHeader, RenderingPluginSourceCPP, RenderingPluginSourceCMake
        };
     
        ctx.AddFileToCopy(RenderingPluginSourceHeader, RenderingPluginDestinationHeader);
        ctx.AddFileToCopy(RenderingPluginSourceCPP, RenderingPluginDestinationCPP);
        ctx.AddFileToCopy(RenderingPluginSourceCMake, RenderingPluginDestinationCMake);

        return ctx;
    }

    public override void OnModifyAndroidProjectFiles(AndroidProjectFiles projectFiles)
    {
    }
}

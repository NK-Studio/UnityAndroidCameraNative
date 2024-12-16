package gmy.camera;

import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLES30;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.renderscript.RenderScript;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import java.nio.IntBuffer;
import java.util.List;

public class CameraPluginActivity extends UnityPlayerActivity
{
    private static final int REQUEST_PERMISSION = 233;
    public static CameraPluginActivity _context;
    private boolean _update;
    private static final int MAX_IMAGES = 4;
    private static final int CONVERSION_FRAME_RATE = 60;
    private final Size _previewSize = new Size(1280, 720);
    private CameraDevice _cameraDevice;
    private CameraCaptureSession _captureSession;
    private ImageReader _imagePreviewReader;
    private RenderScript _renderScript;
    private YuvToRgb _conversionScript;
    private Surface _previewSurface;
    private HandlerThread _handlerThread;

    private static final String CAMERA_PERMISSION = "android.permission.CAMERA";

    /// 카메라 상태 콜백
    private final StateCallback _cameraStateCallback = new StateCallback()
    {
        public void onOpened(CameraDevice camera)
        {
            _cameraDevice = camera;
            setupCameraDevice();
        }

        public void onDisconnected(CameraDevice camera)
        {
            Log.w("CameraPluginActivity", "CameraDevice.StateCallback onDisconnected");
            _cameraDevice.close();
        }

        public void onError(CameraDevice camera, int error)
        {
            Log.e("CameraPluginActivity", "CameraDevice.StateCallback onError[" + error + "]");
            _cameraDevice.close();
            _cameraDevice = null;
        }
    };

    private android.hardware.camera2.CameraCaptureSession.StateCallback _sessionStateCallback = new android.hardware.camera2.CameraCaptureSession.StateCallback()
    {
        public void onConfigured(CameraCaptureSession session)
        {
            _captureSession = session;

            try
            {
                session.setRepeatingRequest(CameraPluginActivity.this.createCaptureRequest(), (CaptureCallback) null, (Handler) null);
            }
            catch (CameraAccessException var3)
            {
                var3.printStackTrace();
            }

        }

        public void onConfigureFailed(CameraCaptureSession session)
        {
            Log.e("CameraPluginActivity", "CameraCaptureSession.StateCallback onConfigureFailed");
        }
    };

    public native void nativeInit();

    public native void nativeRelease();

    protected void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);
        commonInit();
    }

    private void commonInit()
    {
        if (checkPermission(CAMERA_PERMISSION, REQUEST_PERMISSION))
        {
            // 라이브러리 로드
            System.loadLibrary("NativeCameraPlugin");

            nativeInit();
            setContext(this);
            _renderScript = RenderScript.create(UnityPlayer.currentContext);
            _handlerThread = new HandlerThread("CameraPluginActivity");
            _handlerThread.start();

            // 카메라 시작
            startCamera();
        }
    }

    /// CameraPluginActivity 인스턴스 반환
    private static synchronized void setContext(CameraPluginActivity context)
    {
        _context = context;
    }

    /// 권한 체크
    private boolean checkPermission(String permission, int requestCode)
    {

        if (UnityPlayer.currentActivity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
        {
            if (UnityPlayer.currentActivity.shouldShowRequestPermissionRationale(permission))
                finish();
            else
                UnityPlayer.currentActivity.requestPermissions(new String[]{permission, "android.permission.WRITE_EXTERNAL_STORAGE"}, requestCode);

            return false;
        }
        else
            return true;
    }

    /// 권한 요청 결과 처리
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (requestCode == REQUEST_PERMISSION)
        {
            if (grantResults.length > 0 && grantResults[0] == 0)
                commonInit();
            else
                finish();
        }
        else
            finish();
    }

    public void onResume()
    {
        super.onResume();
    }

    protected void onDestroy()
    {
        super.onDestroy();
        nativeRelease();
        setContext(null);
    }

    public void onPause()
    {
        _handlerThread.quitSafely();

        try
        {
            _handlerThread.join();
            _handlerThread = null;
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }

        stopCamera();
        super.onPause();
    }

    private void requestJavaRendering(int texturePointer)
    {
        if (_update)
        {
            int[] imageBuffer = new int[0];
            if (_conversionScript != null)
            {
                imageBuffer = _conversionScript.getOutputBuffer();
            }

            if (imageBuffer.length > 1)
            {
                GLES30.glBindTexture(3553, texturePointer);
                GLES30.glTexSubImage2D(3553, 0, 0, 0, _previewSize.getWidth(), _previewSize.getHeight(), 6408, 5121, IntBuffer.wrap(imageBuffer));
            }
        }
    }

    private void setupCameraDevice()
    {
        try
        {
            if (_previewSurface != null)
                _cameraDevice.createCaptureSession(List.of(_previewSurface), _sessionStateCallback, null);
            else
                Log.e("CameraPluginActivity", "u3d java failed creating preview surface");

        }
        catch (CameraAccessException exception)
        {
            exception.printStackTrace();
        }

    }

    private int getSensorOrientation(String cameraId) throws CameraAccessException
    {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
        return characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
    }

    private CaptureRequest createCaptureRequest()
    {
        Log.e("nkppap", "u3d java createCaptureRequest ... ");

        try
        {
            Builder builder = _cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);

            builder.addTarget(_previewSurface);

            // FPS 범위를 60fps로 설정
            CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
            String pickedCamera = getCamera(manager);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(pickedCamera);

            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            // 1920x1080 해상도 확인
            Size[] sizes = map.getOutputSizes(ImageFormat.PRIVATE);
            for (Size size : sizes)
            {
                Log.e("nkppap", "Supported Size: " + size);
            }

            // FPS 범위 확인
            Range<Integer>[] fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            for (Range<Integer> range : fpsRanges)
            {
                Log.e("nkppap", "Supported FPS range: " + range);
            }

            // 가능한 FPS 범위에서 최대 60fps가 포함된 범위를 선택
            for (Range<Integer> range : fpsRanges)
            {
                Log.e("CameraPluginActivity", "FPS range: " + range);
                if (range.getUpper() == 60)
                {
                    builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, range);
                    Log.e("CameraPluginActivity", "Set FPS range: " + range);
                    break;
                }
            }

            int sensorOrientation = getSensorOrientation(pickedCamera); // 센서 방향 가져오기
            int deviceRotation = getWindowManager().getDefaultDisplay().getRotation(); // 기기 방향 가져오기
            int totalRotation = (sensorOrientation + deviceRotation * 90) % 360;

            builder.set(CaptureRequest.JPEG_ORIENTATION, totalRotation);

            return builder.build();
        }
        catch (CameraAccessException exception)
        {
            exception.printStackTrace();
            return null;
        }


    }

    private void startCamera()
    {
        CameraManager manager = (CameraManager) getSystemService(UnityPlayerActivity.CAMERA_SERVICE);

        try
        {
            // 권한이 요구됨
            if (checkSelfPermission(CAMERA_PERMISSION) != PackageManager.PERMISSION_GRANTED)
                return;

            // 카메라를 찾아서 오픈
            String pickedCamera = getCamera(manager);
            manager.openCamera(pickedCamera, _cameraStateCallback, null);

            // 이미지 미리보기 생성
            int previewHeight = _previewSize.getHeight();
            int previewWidth = _previewSize.getWidth();
            _imagePreviewReader = ImageReader.newInstance(previewWidth, previewHeight, ImageFormat.PRIVATE, 4);

            // yuv to rgb 변환 스크립트 생성
            _conversionScript = new YuvToRgb(_renderScript, _previewSize, CONVERSION_FRAME_RATE);
            _conversionScript.setOutputSurface(_imagePreviewReader.getSurface());

            // 변환한 이미지를 서피스로 지정
            _previewSurface = _conversionScript.getInputSurface();

        }
        catch (CameraAccessException | SecurityException exception)
        {
            exception.printStackTrace();
        }

    }

    private void stopCamera()
    {
        try
        {
            _captureSession.abortCaptures();
            _captureSession.close();
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }

        try
        {
            Image image = _imagePreviewReader.acquireLatestImage();

            if (image != null)
                image.close();

        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
        finally
        {
            if (_imagePreviewReader != null)
            {
                _imagePreviewReader.close();
                _imagePreviewReader = null;
            }
        }

        try
        {
            _cameraDevice.close();
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }

        _conversionScript = null;
    }

    /// 카메라를 가져옵니다.
    private String getCamera(CameraManager manager)
    {
        try
        {
            String[] var2 = manager.getCameraIdList();
            int var3 = var2.length;

            for (int var4 = 0; var4 < var3; ++var4)
            {
                String cameraId = var2[var4];
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                int cameraOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);

                if (cameraOrientation == 1)
                    return cameraId;
            }
        }
        catch (CameraAccessException exception)
        {
            exception.printStackTrace();
        }

        return null;
    }

    @UsedThroughReflection
    public void enablePreviewUpdater(boolean update)
    {
        _update = update;
    }
}

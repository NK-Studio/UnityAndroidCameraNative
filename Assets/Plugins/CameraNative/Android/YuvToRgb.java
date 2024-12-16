package gmy.camera;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Type;
import android.renderscript.Allocation.OnBufferAvailableListener;
import android.renderscript.Type.Builder;
import android.util.Size;
import android.view.Surface;

public class YuvToRgb implements OnBufferAvailableListener
{
    private Allocation _inputAllocation;
    private Allocation _outputAllocation;
    private Allocation _outputAllocationInt;
    private Size _inputSize;
    private ScriptC_yuv2rgb _scriptYuv2Rgb;
    private int[] _outBufferInt;
    private long _lastProcessed;
    private final int _frameEveryMs;

    YuvToRgb(RenderScript rs, Size dimensions, int frameMs)
    {
        _inputSize = dimensions;
        _frameEveryMs = frameMs;
        createAllocations(rs);
        _inputAllocation.setOnBufferAvailableListener(this);
        _scriptYuv2Rgb = new ScriptC_yuv2rgb(rs);
        _scriptYuv2Rgb.set_gCurrentFrame(this._inputAllocation);
        _scriptYuv2Rgb.set_gIntFrame(this._outputAllocationInt);
    }

    private void createAllocations(RenderScript rs)
    {
        int width = this._inputSize.getWidth();
        int height = this._inputSize.getHeight();
        _outBufferInt = new int[width * height];
        Builder yuvTypeBuilder = new Builder(rs, Element.YUV(rs));
        yuvTypeBuilder.setX(width);
        yuvTypeBuilder.setY(height);
        yuvTypeBuilder.setYuvFormat(35);
        _inputAllocation = Allocation.createTyped(rs, yuvTypeBuilder.create(), 33);
        Type rgbType = Type.createXY(rs, Element.RGBA_8888(rs), width, height);
        Type intType = Type.createXY(rs, Element.U32(rs), width, height);
        _outputAllocation = Allocation.createTyped(rs, rgbType, 65);
        _outputAllocationInt = Allocation.createTyped(rs, intType, 1);
    }

    Surface getInputSurface()
    {
        return _inputAllocation.getSurface();
    }

    void setOutputSurface(Surface output)
    {
        _outputAllocation.setSurface(output);
    }

    public void onBufferAvailable(Allocation a)
    {
        _inputAllocation.ioReceive();
        long current = System.currentTimeMillis();
        if (current - _lastProcessed >= (long) _frameEveryMs)
        {
            _scriptYuv2Rgb.forEach_yuv2rgbFrames(_outputAllocation);
            _outputAllocationInt.copyTo(_outBufferInt);
            _lastProcessed = current;
        }

    }

    public int[] getOutputBuffer()
    {
        return _outBufferInt;
    }
}

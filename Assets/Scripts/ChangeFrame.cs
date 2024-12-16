using UnityEngine;

public class ChangeFrame : MonoBehaviour
{
    private void Start()
    {
        QualitySettings.vSyncCount = 0;
        Application.targetFrameRate = (int)Screen.currentResolution.refreshRateRatio.numerator;
        Debug.Log((int)Screen.currentResolution.refreshRateRatio.numerator);
    }
}

using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.Rendering;
using UnityEngine.Serialization;

public class CameraBackground : MonoBehaviour
{
    // public Shader m_shader;
    // Texture m_texture;
    // CommandBuffer m_CommandBuffer;
    // Camera m_Camera;
    public Material TargetMaterial;

    // private void Awake()
    // {
    //      m_Camera = GetComponent<Camera>();
    // }

    public void SetTexture(Texture texture)
    {
        // m_texture = texture;
        // if(m_CommandBuffer == null)
        // {
        //     m_CommandBuffer = new CommandBuffer();
        //     configCommandBuffer();
        // }
        TargetMaterial.SetTexture("_MainTex", texture);
    }

    // private void configCommandBuffer()
    // {
    //     Debug.Log("configCommandBuffer");
    //     m_CommandBuffer.ClearRenderTarget(true, true, Color.clear);
    //     m_CommandBuffer.Blit(m_texture, BuiltinRenderTextureType.CameraTarget, m_material);
    //     m_Camera.AddCommandBuffer(CameraEvent.BeforeForwardOpaque, m_CommandBuffer);
    //     m_Camera.AddCommandBuffer(CameraEvent.BeforeGBuffer, m_CommandBuffer);
    // }




}

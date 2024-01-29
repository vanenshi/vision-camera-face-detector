package com.visioncamerafacedetector;

import androidx.annotation.NonNull;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.mrousavy.camera.frameprocessor.FrameProcessorPluginRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VisionCameraFaceDetectorPackage implements ReactPackage {
  static {
    FrameProcessorPluginRegistry.addFrameProcessorPlugin(
      VisionCameraFaceDetectorPlugin.NAME,
      VisionCameraFaceDetectorPlugin::new
    );
  }

  @NonNull
  @Override
  public List<NativeModule> createNativeModules(@NonNull ReactApplicationContext reactContext) {
    return new ArrayList<>();
  }

  @NonNull
  @Override
  public List<ViewManager> createViewManagers(@NonNull ReactApplicationContext reactContext) {
    return Collections.emptyList();
  }
}

package com.visioncamerafacedetector;


import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.mrousavy.camera.frameprocessor.Frame;
import com.mrousavy.camera.frameprocessor.FrameProcessorPlugin;
import com.mrousavy.camera.frameprocessor.VisionCameraProxy;
import com.mrousavy.camera.types.Orientation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class VisionCameraFaceDetectorPlugin extends FrameProcessorPlugin {
  public static final String NAME = "scanFaces";

  FaceDetectorOptions options =
    new FaceDetectorOptions.Builder()
      .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
      .build();
  FaceDetector faceDetector = FaceDetection.getClient(options);

  VisionCameraFaceDetectorPlugin(@NonNull VisionCameraProxy proxy, @Nullable Map<String, Object> options) {
    Log.d(NAME, "CodeScannerProcessorPlugin initialized with options:" + options);
  }


  @NonNull
  private Map<String, Object> processBoundingBox(@NonNull Rect boundingBox) {
    Map<String, Object> bounds = new HashMap<>();

    // Calculate offset (we need to center the overlay on the target)
    double offsetX = (boundingBox.exactCenterX() - (double) boundingBox.width()) / 2.0f;
    double offsetY = (boundingBox.exactCenterY() - (double) boundingBox.height()) / 2.0f;

    double x = boundingBox.right + offsetX;
    double y = boundingBox.top + offsetY;


    bounds.put("x", boundingBox.centerX() + (boundingBox.centerX() - x));
    bounds.put("y", boundingBox.centerY() + (y - boundingBox.centerY()));
    bounds.put("width", boundingBox.width());
    bounds.put("height", boundingBox.height());


    bounds.put("boundingCenterX", boundingBox.centerX());
    bounds.put("boundingCenterY", boundingBox.centerY());

    bounds.put("boundingExactCenterX", (double) boundingBox.exactCenterX());
    bounds.put("boundingExactCenterY", (double) boundingBox.exactCenterY());

    return bounds;
  }

  @NonNull
  private Map<String, Object> processFaceContours(Face face) {
    // All faceContours
    int[] faceContoursTypes =
      new int[]{
        FaceContour.FACE,
        FaceContour.LEFT_EYEBROW_TOP,
        FaceContour.LEFT_EYEBROW_BOTTOM,
        FaceContour.RIGHT_EYEBROW_TOP,
        FaceContour.RIGHT_EYEBROW_BOTTOM,
        FaceContour.LEFT_EYE,
        FaceContour.RIGHT_EYE,
        FaceContour.UPPER_LIP_TOP,
        FaceContour.UPPER_LIP_BOTTOM,
        FaceContour.LOWER_LIP_TOP,
        FaceContour.LOWER_LIP_BOTTOM,
        FaceContour.NOSE_BRIDGE,
        FaceContour.NOSE_BOTTOM,
        FaceContour.LEFT_CHEEK,
        FaceContour.RIGHT_CHEEK
      };

    String[] faceContoursTypesStrings = {
      "FACE",
      "LEFT_EYEBROW_TOP",
      "LEFT_EYEBROW_BOTTOM",
      "RIGHT_EYEBROW_TOP",
      "RIGHT_EYEBROW_BOTTOM",
      "LEFT_EYE",
      "RIGHT_EYE",
      "UPPER_LIP_TOP",
      "UPPER_LIP_BOTTOM",
      "LOWER_LIP_TOP",
      "LOWER_LIP_BOTTOM",
      "NOSE_BRIDGE",
      "NOSE_BOTTOM",
      "LEFT_CHEEK",
      "RIGHT_CHEEK"
    };

    Map<String, Object> faceContoursTypesMap = new HashMap<>();

    for (int i = 0; i < faceContoursTypesStrings.length; i++) {
      FaceContour contour = face.getContour(faceContoursTypes[i]);
      List<PointF> points = contour.getPoints();
      List<Object> pointsArray = new ArrayList<>();


      for (int j = 0; j < points.size(); j++) {
        Map<String, Object> currentPointsMap = new HashMap<>();

        currentPointsMap.put("x", (double) points.get(j).x);
        currentPointsMap.put("y", (double) points.get(j).y);

        pointsArray.add(currentPointsMap);
      }
      faceContoursTypesMap.put(faceContoursTypesStrings[contour.getFaceContourType() - 1], pointsArray);
    }

    return faceContoursTypesMap;
  }

  @Nullable
  @Override
  public Object callback(@NonNull Frame frame, @Nullable Map<String, Object> arguments) {
    Image mediaImage = frame.getImage();

    // Check if the image is in a supported format
    int format = mediaImage.getFormat();
    if (format != ImageFormat.YUV_420_888) {
      Log.e(
        NAME,
        "Unsupported image format: " +
          format +
          ". Only YUV_420_888 is supported for now."
      );
      return null;
    }

    Orientation orientation = frame.getOrientation();
    InputImage image = InputImage.fromMediaImage(
      mediaImage, 90
    );

    List<Object> array = new ArrayList<>();
    Task<List<Face>> task = faceDetector.process(image);

    try {
      List<Face> faces = Tasks.await(task);

      for (Face face : faces) {
        Map<String, Object> map = new HashMap<>();

        map.put("rollAngle", (double) face.getHeadEulerAngleZ()); // Head is rotated to the left rotZ degrees
        map.put("pitchAngle", (double) face.getHeadEulerAngleX()); // Head is rotated to the right rotX degrees
        map.put("yawAngle", (double) face.getHeadEulerAngleY());  // Head is tilted sideways rotY degrees

        Float leftEyeOpenProbability = face.getLeftEyeOpenProbability();
        Float rightEyeOpenProbability = face.getRightEyeOpenProbability();
        Float smilingProbability = face.getSmilingProbability();

        map.put("leftEyeOpenProbability", leftEyeOpenProbability == null ? null : leftEyeOpenProbability.doubleValue());
        map.put("rightEyeOpenProbability", rightEyeOpenProbability == null ? null : rightEyeOpenProbability.doubleValue());
        map.put("smilingProbability", smilingProbability == null ? null : smilingProbability.doubleValue());

        Map<String, Object> contours = processFaceContours(face);
        Map<String, Object> bounds = processBoundingBox(face.getBoundingBox());

        map.put("bounds", bounds);
        map.put("contours", contours);

        array.add(map);
      }
    } catch (Exception e) {
      Log.e(NAME, e.getMessage());
      return null;
    }

    return array;
  }
}

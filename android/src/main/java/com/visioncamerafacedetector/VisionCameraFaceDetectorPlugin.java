package com.visioncamerafacedetector;


import static java.lang.Math.ceil;

import android.graphics.PointF;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
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
  private WritableMap processBoundingBox(@NonNull Rect boundingBox) {
    WritableMap bounds = Arguments.createMap();

    // Calculate offset (we need to center the overlay on the target)
    Double offsetX = (boundingBox.exactCenterX() - ceil(boundingBox.width())) / 2.0f;
    Double offsetY = (boundingBox.exactCenterY() - ceil(boundingBox.height())) / 2.0f;

    Double x = boundingBox.right + offsetX;
    Double y = boundingBox.top + offsetY;


    bounds.putDouble("x", boundingBox.centerX() + (boundingBox.centerX() - x));
    bounds.putDouble("y", boundingBox.centerY() + (y - boundingBox.centerY()));
    bounds.putDouble("width", boundingBox.width());
    bounds.putDouble("height", boundingBox.height());


    bounds.putDouble("boundingCenterX", boundingBox.centerX());
    bounds.putDouble("boundingCenterY", boundingBox.centerY());
    bounds.putDouble("boundingExactCenterX", boundingBox.exactCenterX());
    bounds.putDouble("boundingExactCenterY", boundingBox.exactCenterY());

    return bounds;
  }

  @NonNull
  private WritableMap  processFaceContours(Face face) {
    // All faceContours
    int[] faceContoursTypes =
      new int[] {
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

    WritableMap faceContoursTypesMap = Arguments.createMap();

    for (int i = 0; i < faceContoursTypesStrings.length; i++) {
      FaceContour contour = face.getContour(faceContoursTypes[i]);
      List<PointF> points = contour.getPoints();
      WritableArray pointsArray = Arguments.createArray();

      for (int j = 0; j < points.size(); j++) {
        WritableMap currentPointsMap = Arguments.createMap();

        currentPointsMap.putDouble("x", points.get(j).x);
        currentPointsMap.putDouble("y", points.get(j).y);

        pointsArray.pushMap(currentPointsMap);
      }
      faceContoursTypesMap.putArray(faceContoursTypesStrings[contour.getFaceContourType() - 1], pointsArray);
    }

    return faceContoursTypesMap;
  }


  @Nullable
  @Override
  public Object callback(@NonNull Frame frame, @Nullable Map<String, Object> arguments) {
    Image mediaImage = frame.getImage();

    // Check if the image is in a supported format
//    int format = mediaImage.getFormat();
//    if (format != ImageFormat.YUV_420_888) {
//      Log.e(
//        NAME,
//        "Unsupported image format: " +
//          format +
//          ". Only YUV_420_888 is supported for now."
//      );
//      return null;
//    }


    Orientation orientation = frame.getOrientation();
    InputImage image = InputImage.fromMediaImage(
      mediaImage,
      270
    );

    List<Object> array = new ArrayList<>();
    Task<List<Face>> task = faceDetector.process(image);

    try {
      List<Face> faces = Tasks.await(task);

      for (Face face : faces) {
        WritableMap map = Arguments.createMap();

        map.putDouble("rollAngle", face.getHeadEulerAngleZ()); // Head is rotated to the left rotZ degrees
        map.putDouble("pitchAngle", face.getHeadEulerAngleX()); // Head is rotated to the right rotX degrees
        map.putDouble("yawAngle", face.getHeadEulerAngleY());  // Head is tilted sideways rotY degrees
        map.putDouble("leftEyeOpenProbability", face.getLeftEyeOpenProbability());
        map.putDouble("rightEyeOpenProbability", face.getRightEyeOpenProbability());
        map.putDouble("smilingProbability", face.getSmilingProbability());


        WritableMap contours = processFaceContours(face);
        WritableMap bounds = processBoundingBox(face.getBoundingBox());

        map.putMap("bounds", bounds);
        map.putMap("contours", contours);

        array.add(map);
      }
    } catch (Exception e) {
      Log.e(NAME, e.getMessage());
      return null;
    }

    return array;
  }
}

import { VisionCameraProxy, type Frame } from 'react-native-vision-camera';

const plugin = VisionCameraProxy.initFrameProcessorPlugin('scanFaces');

type Point = { x: number; y: number };
export interface Face {
  leftEyeOpenProbability: number;
  rollAngle: number;
  pitchAngle: number;
  yawAngle: number;
  rightEyeOpenProbability: number;
  smilingProbability: number;
  bounds: {
    y: number;
    x: number;
    height: number;
    width: number;
  };
  contours: {
    FACE: Point[];
    NOSE_BOTTOM: Point[];
    LOWER_LIP_TOP: Point[];
    RIGHT_EYEBROW_BOTTOM: Point[];
    LOWER_LIP_BOTTOM: Point[];
    NOSE_BRIDGE: Point[];
    RIGHT_CHEEK: Point[];
    RIGHT_EYEBROW_TOP: Point[];
    LEFT_EYEBROW_TOP: Point[];
    UPPER_LIP_BOTTOM: Point[];
    LEFT_EYEBROW_BOTTOM: Point[];
    UPPER_LIP_TOP: Point[];
    LEFT_EYE: Point[];
    RIGHT_EYE: Point[];
    LEFT_CHEEK: Point[];
  };
}

export function scanFaces(frame: Frame): Face[] {
  'worklet';
  if (plugin == null) {
    throw new Error('Failed to load Frame Processor Plugin!');
  }
  var faces = plugin.call(frame) as Record<string, any>;

  if (!Array.isArray(faces)) {
    console.warn('Native frame processor failed to return a proper array!');
    return [];
  }

  return faces.map((face) => face);
}

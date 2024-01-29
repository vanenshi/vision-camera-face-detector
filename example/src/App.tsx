import * as React from 'react';
import { StyleSheet } from 'react-native';
import Animated, {
  useAnimatedStyle,
  useSharedValue,
} from 'react-native-reanimated';
import {
  Camera,
  useCameraDevice,
  useFrameProcessor,
  runAtTargetFps,
} from 'react-native-vision-camera';
import { Worklets } from 'react-native-worklets-core';
import { scanFaces, type Face } from 'vision-camera-face-detector';
import { Dimensions } from 'react-native';

const dimensions = Dimensions.get('window');

export default function App() {
  const [hasPermission, setHasPermission] = React.useState(false);
  const face = useSharedValue<Face | undefined>(undefined);

  const device = useCameraDevice('front');

  React.useEffect(() => {
    (async () => {
      const status = await Camera.requestCameraPermission();
      setHasPermission(status === 'granted');
    })();
  }, []);

  const updateFace = Worklets.createRunInJsFn((_face?: Face) => {
    face.value = _face;
  });

  const frameProcessor = useFrameProcessor((frame) => {
    'worklet';
    runAtTargetFps(2, () => {
      'worklet';
      var startTime = performance.now();
      const scannedFaces = scanFaces(frame);
      var endTime = performance.now();
      console.log(
        `Call to doSomething took ${endTime - startTime} milliseconds`
      );
      console.log(scannedFaces);

      const xFactor = dimensions.width / frame.width;
      const yFactor = dimensions.height / frame.height;

      console.log(scannedFaces[0]?.bounds);

      updateFace(
        scannedFaces?.[0] && {
          ...scannedFaces[0],
          bounds: {
            ...scannedFaces[0].bounds,
            x: scannedFaces[0].bounds.x * xFactor,
            y: scannedFaces[0].bounds.y * yFactor,
            height: scannedFaces[0].bounds.height * yFactor,
            width: scannedFaces[0].bounds.width * xFactor,
          },
        }
      );
    });
  }, []);

  const viewStyle = useAnimatedStyle(() => {
    return {
      position: 'absolute',
      width: face.value?.bounds.width,
      height: face.value?.bounds.height,
      top: face.value?.bounds.y,
      left: face.value?.bounds.x,
      borderColor: 'green',
      borderWidth: 2,
    };
  }, [face]);

  return (
    <>
      {device != null && hasPermission ? (
        <Camera
          style={StyleSheet.absoluteFill}
          device={device}
          isActive={true}
          frameProcessor={frameProcessor}
          pixelFormat="yuv"
        />
      ) : null}

      <Animated.View style={viewStyle} />
    </>
  );
}

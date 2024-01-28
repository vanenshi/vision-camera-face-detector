import * as React from 'react';
import { StyleSheet } from 'react-native';
import {
  Camera,
  useCameraDevice,
  useFrameProcessor,
} from 'react-native-vision-camera';
import { scanFaces, type Face } from 'vision-camera-face-detector';

export default function App() {
  const [hasPermission, setHasPermission] = React.useState(false);
  const [faces, _] = React.useState<Face[]>([]);

  const device = useCameraDevice('front');

  React.useEffect(() => {
    console.log(faces);
  }, [faces]);

  React.useEffect(() => {
    (async () => {
      const status = await Camera.requestCameraPermission();
      setHasPermission(status === 'granted');
    })();
  }, []);

  // const frameProcessor = useFrameProcessor((frame) => {
  //   'worklet';
  //   const scannedFaces = scanFaces(frame);
  //   runOnJS(setFaces)(scannedFaces);
  // }, []);
  const frameProcessor = useFrameProcessor((frame) => {
    'worklet';
    // console.log(
    //   `${frame.timestamp}: ${frame.width}x${frame.height} ${frame.pixelFormat} Frame (${frame.orientation})`
    // );
    const scannedFaces = scanFaces(frame);
    console.log(`Detected ${scannedFaces?.length} faces!`);
    // setFaces(scannedFaces);
  }, []);

  return device != null && hasPermission ? (
    <Camera
      style={StyleSheet.absoluteFill}
      device={device}
      isActive={true}
      frameProcessor={frameProcessor}
    />
  ) : null;
}

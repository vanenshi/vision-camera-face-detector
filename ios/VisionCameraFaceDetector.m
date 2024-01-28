#import <Foundation/Foundation.h>
#import <VisionCamera/FrameProcessorPlugin.h>
#import <VisionCamera/FrameProcessorPluginRegistry.h>
#import <VisionCamera/Frame.h>

#if defined __has_include && __has_include("VisionCameraFaceDetector-Swift.h")
#import "VisionCameraFaceDetector-Swift.h"
#else
#import <VisionCameraFaceDetector/VisionCameraFaceDetector-Swift.h>
#endif



@interface VisionCameraFaceDetector(FrameProcessorPluginLoader)
@end

@implementation VisionCameraFaceDetector(FrameProcessorPluginLoader)

+ (void)load {
    [FrameProcessorPluginRegistry addFrameProcessorPlugin:@"scanFaces"
                                          withInitializer:^FrameProcessorPlugin* _Nonnull(VisionCameraProxyHolder* _Nonnull proxy,
                                                                                          NSDictionary* _Nullable options) {
        return [[VisionCameraFaceDetector alloc] initWithProxy:proxy withOptions:options];
    }];
}

@end


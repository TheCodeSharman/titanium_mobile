// UIImage+RoundedCorner.m
// Created by Trevor Harmon on 9/20/09.
// Free for personal or commercial use, with or without modification.
// No warranty is expressed or implied.

#import "UIImage+Alpha.h"
#import "UIImage+RoundedCorner.h"

@implementation UIImageRoundedCorner

// Adds a rectangular path to the given context and rounds its corners by the given extents
// Original author: Björn Sållarp. Used with permission. See: http://blog.sallarp.com/iphone-uiimage-round-corners/
+ (void)addRoundedRectToPath:(CGRect)rect context:(CGContextRef)context ovalWidth:(CGFloat)ovalWidth ovalHeight:(CGFloat)ovalHeight
{
  if (ovalWidth == 0 || ovalHeight == 0) {
    CGContextAddRect(context, rect);
    return;
  }
  CGContextSaveGState(context);
  CGContextTranslateCTM(context, CGRectGetMinX(rect), CGRectGetMinY(rect));
  CGContextScaleCTM(context, ovalWidth, ovalHeight);
  CGFloat fw = CGRectGetWidth(rect) / ovalWidth;
  CGFloat fh = CGRectGetHeight(rect) / ovalHeight;
  CGContextMoveToPoint(context, fw, fh / 2);
  CGContextAddArcToPoint(context, fw, fh, fw / 2, fh, 1);
  CGContextAddArcToPoint(context, 0, fh, 0, fh / 2, 1);
  CGContextAddArcToPoint(context, 0, 0, fw / 2, 0, 1);
  CGContextAddArcToPoint(context, fw, 0, fw, fh / 2, 1);
  CGContextClosePath(context);
  CGContextRestoreGState(context);
}

// Creates a copy of this image with rounded corners
// If borderSize is non-zero, a transparent border of the given size will also be added
// Original author: Björn Sållarp. Used with permission. See: http://blog.sallarp.com/iphone-uiimage-round-corners/
+ (UIImage *)roundedCornerImage:(NSInteger)cornerSize borderSize:(NSInteger)borderSize image:(UIImage *)image_
{
  // If the image does not have an alpha layer, add one
  UIImage *image = [UIImageAlpha imageWithAlpha:image_];

  CGFloat scale = MAX(image.scale, 1.0f);
  NSUInteger scaledBorderSize = borderSize * scale;
  CGFloat scaledWidth = image.size.width * scale;
  CGFloat scaledHeight = image.size.height * scale;

  // Build a context that's the same dimensions as the new size (width + double border, height + double border)
  CGContextRef context = CGBitmapContextCreate(NULL,
      scaledWidth + 2 * scaledBorderSize,
      scaledHeight + 2 * scaledBorderSize,
      CGImageGetBitsPerComponent(image.CGImage),
      0,
      CGImageGetColorSpace(image.CGImage),
      CGImageGetBitmapInfo(image.CGImage));

  // Create a clipping path with rounded corners
  CGContextBeginPath(context);
  [self addRoundedRectToPath:CGRectMake(scaledBorderSize, scaledBorderSize, scaledWidth, scaledHeight)
                     context:context
                   ovalWidth:cornerSize * scale
                  ovalHeight:cornerSize * scale];
  CGContextClosePath(context);
  CGContextClip(context);

  // Draw the image to the context; the clipping path will make anything outside the rounded rect transparent
  CGContextDrawImage(context, CGRectMake(scaledBorderSize, scaledBorderSize, scaledWidth, scaledHeight), image.CGImage);

  // Create a CGImage from the context
  CGImageRef clippedImage = CGBitmapContextCreateImage(context);
  CGContextRelease(context);

  // Create a UIImage from the CGImage
  UIImage *roundedImage = [UIImage imageWithCGImage:clippedImage scale:image.scale orientation:UIImageOrientationUp];

  CGImageRelease(clippedImage);

  return roundedImage;
}

@end

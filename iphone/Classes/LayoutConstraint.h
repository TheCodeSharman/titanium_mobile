/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import <UIKit/UIKit.h>
#import "TiDimension.h"
#import "TiUtils.h"

@protocol LayoutAutosizing

@optional

-(CGFloat)minimumParentWidthForWidth:(CGFloat)suggestedWidth;
-(CGFloat)minimumParentHeightForWidth:(CGFloat)suggestedWidth;

-(CGFloat)autoWidthForWidth:(CGFloat)suggestedWidth;
-(CGFloat)autoHeightForWidth:(CGFloat)width;


-(CGFloat)verifyWidth:(CGFloat)suggestedWidth;
-(CGFloat)verifyHeight:(CGFloat)suggestedHeight;

-(UIViewAutoresizing)verifyAutoresizing:(UIViewAutoresizing)suggestedResizing;

@end

typedef enum {
	TiLayoutRuleAbsolute,
	TiLayoutRuleVertical,
	TiLayoutRuleHorizontal,
} TiLayoutRule;

TI_INLINE CGFloat TiFixedValueRuleFromObject(id object)
{
	return [TiUtils floatValue:object def:0];
}

TI_INLINE TiLayoutRule TiLayoutRuleFromObject(id object)
{
	if ([object isKindOfClass:[NSString class]])
	{
		if ([object caseInsensitiveCompare:@"vertical"]==NSOrderedSame)
		{
			return TiLayoutRuleVertical;
		}
		if ([object caseInsensitiveCompare:@"horizontal"]==NSOrderedSame)
		{
			return TiLayoutRuleHorizontal;
		}
	}
	return TiLayoutRuleAbsolute;
}

TI_INLINE BOOL TiLayoutRuleIsAbsolute(TiLayoutRule rule)
{
	return rule==TiLayoutRuleAbsolute;
}

TI_INLINE BOOL TiLayoutRuleIsVertical(TiLayoutRule rule)
{
	return rule==TiLayoutRuleVertical;
}

TI_INLINE BOOL TiLayoutRuleIsHorizontal(TiLayoutRule rule)
{
	return rule==TiLayoutRuleHorizontal;
}


typedef struct LayoutConstraint {

	TiDimension centerX;
	TiDimension left;
	TiDimension right;
	TiDimension width;
	
	TiDimension centerY;
	TiDimension top;
	TiDimension bottom;
	TiDimension height;
	
	TiDimension paddingLeft;
	TiDimension paddingRight;
	TiDimension paddingTop;
	TiDimension paddingBottom;

	TiDimension marginLeft;
	TiDimension marginRight;
	TiDimension marginTop;
	TiDimension marginBottom;

	TiLayoutRule layoutStyle;
	
	CGFloat minimumHeight;
	CGFloat minimumWidth;
	
} LayoutConstraint;

@class TiUIView;
@class TiViewProxy;
void ApplyConstraintToViewWithBounds(LayoutConstraint * constraint, TiUIView * subView, CGRect viewBounds);
CGFloat WidthFromConstraintGivenWidth(LayoutConstraint * constraint,CGFloat viewWidth);
CGSize SizeConstraintViewWithSizeAddingResizing(LayoutConstraint * constraint, NSObject<LayoutAutosizing> * autoSizer, CGSize referenceSize, UIViewAutoresizing * resultResizing);
CGPoint PositionConstraintGivenSizeBoundsAddingResizing(LayoutConstraint * constraint, TiViewProxy* viewProxy, CGSize viewSize, CGPoint anchorPoint, CGSize referenceSize, CGSize sandboxSize, UIViewAutoresizing * resultResizing);
BOOL IsLayoutUndefined(LayoutConstraint *constraint);

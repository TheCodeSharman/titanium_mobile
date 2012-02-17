/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiBase.h"

#define INCH_IN_CM 2.54
#define INCH_IN_MM 25.4


//Not a class for speed reasons, like LayoutConstraint.

typedef enum {
	TiDimensionTypeUndefined,
	TiDimensionTypeDip,
	TiDimensionTypeAuto,
	TiDimensionTypePercent,
} TiDimensionType;

struct TiDimension {
	TiDimensionType type;
	CGFloat value;
	//If type is TiDimensionTypeDip, value is a Dip constant,
	//If type is TiDimensionTypePercent, value ranges from 0 (0%) to 1.0 (100%)
};

typedef struct TiDimension TiDimension;

extern const TiDimension TiDimensionZero;
extern const TiDimension TiDimensionAuto;
extern const TiDimension TiDimensionUndefined;

TiDimension TiDimensionMake(TiDimensionType type, CGFloat value);
CGFloat convertInchToPixels(CGFloat value);
CGFloat convertPixelsToDip(CGFloat value);

TI_INLINE TiDimension TiDimensionDip(CGFloat value)
{
	return TiDimensionMake(TiDimensionTypeDip,value);
}

TI_INLINE bool TiDimensionIsPercent(TiDimension dimension)
{
	return dimension.type == TiDimensionTypePercent;
}

TI_INLINE bool TiDimensionIsAuto(TiDimension dimension)
{
	return dimension.type == TiDimensionTypeAuto;
}

TI_INLINE bool TiDimensionIsDip(TiDimension dimension)
{
	return dimension.type == TiDimensionTypeDip;
}

TI_INLINE bool TiDimensionIsUndefined(TiDimension dimension)
{
	return dimension.type == TiDimensionTypeUndefined;
}

TI_INLINE bool TiDimensionEqual(TiDimension dimension1, TiDimension dimension2)
{
	if (dimension1.type != dimension2.type)
	{
		return false;
	}
	if (TiDimensionIsDip(dimension1) || TiDimensionIsPercent(dimension1)) {
		//Value is only valid in pixels and percent. In undefined and auto, value is ignored.
		return dimension1.value == dimension2.value;
	}
	return true;
}

TI_INLINE TiDimension TiDimensionFromObject(id object)
{
	if ([object isKindOfClass:[NSString class]])
	{
		if ([object caseInsensitiveCompare:@"auto"]==NSOrderedSame)
		{
			return TiDimensionAuto;
		}
		// do px vs % parsing
		NSRange range = [object rangeOfString:@"px"];
		if (range.location!=NSNotFound)
		{
			NSString *value = [[object substringToIndex:range.location] stringByReplacingOccurrencesOfString:@" " withString:@""];
			return TiDimensionMake(TiDimensionTypeDip, convertPixelsToDip([value floatValue]));
		}
        range = [object rangeOfString:@"cm"];
        if (range.location!=NSNotFound)
        {
            NSString *value = [[object substringToIndex:range.location] stringByReplacingOccurrencesOfString:@" " withString:@""];
            float dimValue = convertInchToPixels(([value floatValue]/INCH_IN_CM));
            return TiDimensionMake(TiDimensionTypeDip, convertPixelsToDip(dimValue));
            
        }
        range = [object rangeOfString:@"mm"];
        if (range.location!=NSNotFound)
        {
            NSString *value = [[object substringToIndex:range.location] stringByReplacingOccurrencesOfString:@" " withString:@""];
            float dimValue = convertInchToPixels(([value floatValue]/INCH_IN_MM));
            return TiDimensionMake(TiDimensionTypeDip, convertPixelsToDip(dimValue));
            
        }
        range = [object rangeOfString:@"in"];
        if (range.location!=NSNotFound)
        {
            NSString *value = [[object substringToIndex:range.location] stringByReplacingOccurrencesOfString:@" " withString:@""];
            float dimValue = convertInchToPixels([value floatValue]);
            return TiDimensionMake(TiDimensionTypeDip, convertPixelsToDip(dimValue));
            
        }
        range = [object rangeOfString:@"dp"];
        if (range.location==NSNotFound) {
            range = [object rangeOfString:@"dip"];
        }
        if (range.location!=NSNotFound)
        {
            NSString *value = [[object substringToIndex:range.location] stringByReplacingOccurrencesOfString:@" " withString:@""];
            return TiDimensionMake(TiDimensionTypeDip, [value floatValue]);
            
        }    
		range = [object rangeOfString:@"%"];
		if (range.location!=NSNotFound)
		{
			NSString *value = [[object substringToIndex:range.location] stringByReplacingOccurrencesOfString:@" " withString:@""];
			return TiDimensionMake(TiDimensionTypePercent, ([value floatValue] / 100.0));
		}
	}
	if ([object respondsToSelector:@selector(floatValue)])
	{
		return TiDimensionMake(TiDimensionTypeDip, [object floatValue]);
	}
	return TiDimensionUndefined;
}

TI_INLINE BOOL TiDimensionDidCalculateValue(TiDimension dimension,CGFloat boundingValue,CGFloat * result)
{
	switch (dimension.type)
	{
		case TiDimensionTypeDip:
			*result = dimension.value;
			return YES;
		case TiDimensionTypePercent:
			*result = dimension.value * boundingValue;
			return YES;
		default: {
			break;
		}
	}
	return NO;
}

TI_INLINE CGFloat TiDimensionCalculateValue(TiDimension dimension,CGFloat boundingValue)
{
	CGFloat result;
	if(TiDimensionDidCalculateValue(dimension,boundingValue,&result))
	{
		return result;
	}
	return 0.0;
}

TI_INLINE CGFloat TiDimensionCalculateRatio(TiDimension dimension,CGFloat boundingValue)
{
	switch (dimension.type)
	{
		case TiDimensionTypePercent:
			return dimension.value;
		case TiDimensionTypeDip:
			return dimension.value / boundingValue;
		default: {
			break;
		}
	}
	return 0.0;
}

TI_INLINE CGFloat TiDimensionCalculateMargins(TiDimension dimension1, TiDimension dimension2, CGFloat boundingValue)
{
	return boundingValue - (TiDimensionCalculateValue(dimension1, boundingValue) + TiDimensionCalculateValue(dimension2, boundingValue));
}

//TODO: Do these ALL have to be TI_INLINE?
TI_INLINE CGRect TiDimensionLayerContentCenter(TiDimension top, TiDimension left, TiDimension bottom, TiDimension right, CGSize imageSize)
{
	CGRect result;
	result.origin.y = TiDimensionCalculateRatio(top,imageSize.height);
	result.size.height = 1.0 - TiDimensionCalculateRatio(bottom,imageSize.height) - result.origin.y;
	result.origin.x = TiDimensionCalculateRatio(left,imageSize.width);
	result.size.width = 1.0 - TiDimensionCalculateRatio(right,imageSize.width) - result.origin.x;

	return result;
}

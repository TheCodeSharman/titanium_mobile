/*
 * Axway Appcelerator Titanium Mobile
 * Copyright (c) 2011-Present by Axway, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
/* eslint-env mocha */
/* global Ti */
/* eslint no-unused-expressions: "off" */
import should from './utilities/assertions';

describe('ES6 async/await', () => {
	it('handles awaiting on async function from another', finished => {
		async function first() {
			return 1;
		}
		async function second() {
			const result = await first();
			return result + 3;
		}

		second().then(result => {
			result.should.eql(4);
			finished();
		}). catch(err => finished(err));
	});
});
